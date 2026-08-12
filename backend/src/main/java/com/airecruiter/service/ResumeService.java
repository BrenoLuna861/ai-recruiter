package com.airecruiter.service;

import com.airecruiter.config.SlidingWindowRateLimiter;
import com.airecruiter.dto.response.ResumeResponse;
import com.airecruiter.entity.Resume;
import com.airecruiter.entity.ResumeAnalysis;
import com.airecruiter.entity.User;
import com.airecruiter.repository.ResumeAnalysisRepository;
import com.airecruiter.repository.ResumeRepository;
import com.airecruiter.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final AnthropicService anthropicService;
    private final SlidingWindowRateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Análises de aderência por usuário/hora. Cada uma custa crédito da Anthropic. */
    @Value("${resumes.max-matches-por-hora:15}")
    private int maxMatchesPorHora;

    @Transactional
    public ResumeResponse analyzeResume(MultipartFile file, String title, String email) {
        validateFile(file);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String content = extractText(file);

        Resume resume = Resume.builder()
            .user(user)
            .title(title != null ? title : file.getOriginalFilename())
            .fileType(file.getContentType())
            .content(content)
            .status(Resume.Status.ANALYZING)
            .build();
        resumeRepository.save(resume);

        try {
            String analysisJson = anthropicService.analyzeResume(content);
            if (analysisJson == null || analysisJson.isBlank() || "{}".equals(analysisJson.strip())) {
                log.error("Anthropic returned empty analysis for resume {}", resume.getId());
                resume.setStatus(Resume.Status.ERROR);
                resumeRepository.save(resume);
                return toResponse(resume, null);
            }

            String cleanJson = analysisJson.strip();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }
            Map<?, ?> data = objectMapper.readValue(cleanJson, Map.class);

            resume.setSkillsScore(toInt(data.get("skillsScore")));
            resume.setExperienceScore(toInt(data.get("experienceScore")));
            resume.setFormatScore(toInt(data.get("formatScore")));
            resume.setAtsScore(toInt(data.get("atsScore")));

            // A nota geral e CALCULADA, nao pedida a IA. Antes vinha como um numero
            // livre do modelo, sem relacao com as outras quatro — nao dava para
            // explicar ao candidato como 87 saia de 92/88/80/83, e o mesmo curriculo
            // podia receber notas diferentes a cada execucao.
            resume.setOverallScore(calcularNotaGeral(resume));

            // A analise qualitativa vai para o MySQL junto com as notas.
            resume.setAnalysisJson(cleanJson);
            resume.setStatus(Resume.Status.DONE);

            try {
                ResumeAnalysis analysis = ResumeAnalysis.builder()
                    .resumeId(resume.getId())
                    .userId(user.getId())
                    .fullAnalysis((String) data.get("fullAnalysis"))
                    .summary((String) data.get("summary"))
                    .strengths(toStringList(data.get("strengths")))
                    .weaknesses(toStringList(data.get("weaknesses")))
                    .suggestions(toStringList(data.get("suggestions")))
                    .keywordsFound(toStringList(data.get("keywordsFound")))
                    .keywordsMissing(toStringList(data.get("keywordsMissing")))
                    .rewrittenSummary((String) data.get("rewrittenSummary"))
                    .overallScore(resume.getOverallScore())
                    .skillsScore(resume.getSkillsScore())
                    .experienceScore(resume.getExperienceScore())
                    .formatScore(resume.getFormatScore())
                    .atsScore(resume.getAtsScore())
                    .createdAt(LocalDateTime.now())
                    .build();
                analysisRepository.save(analysis);
                resume.setAnalysisMongoId(analysis.getId());
            } catch (Exception mongoEx) {
                log.warn("MongoDB unavailable — detailed analysis not persisted (scores saved to MySQL): {}", mongoEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Analysis error for resume {}: {}", resume.getId(), e.getMessage());
            resume.setStatus(Resume.Status.ERROR);
        }

        resumeRepository.save(resume);
        return toResponse(resume, null);
    }

    /**
     * Gera a versao melhorada do curriculo.
     *
     * O conteudo sai do banco, nao do cliente: o texto ja esta salvo desde a
     * analise, entao nao ha motivo para o navegador reenviar o curriculo inteiro
     * a cada pedido.
     */
    public String improveResume(Long id, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Resume resume = resumeRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Currículo não encontrado"));

        if (resume.getContent() == null || resume.getContent().isBlank()) {
            throw new IllegalArgumentException("Este currículo não tem texto extraído para reescrever.");
        }

        // A reescrita recebe o diagnostico da analise. Sem isso o modelo redescobre
        // tudo do zero e perde achados especificos — por exemplo, a analise mandava
        // remover um dado medico do cabecalho e a reescrita o mantinha.
        String fracos = extrairLista(resume.getAnalysisJson(), "weaknesses");
        String sugestoes = extrairLista(resume.getAnalysisJson(), "suggestions");

        return anthropicService.improveResume(resume.getContent(), fracos, sugestoes);
    }

    /** Transforma um array do JSON da análise numa lista com hífens, para o prompt. */
    private String extrairLista(String analysisJson, String campo) {
        if (analysisJson == null || analysisJson.isBlank()) return "";
        try {
            Map<?, ?> dados = objectMapper.readValue(analysisJson, Map.class);
            List<String> itens = toStringList(dados.get(campo));
            if (itens == null || itens.isEmpty()) return "";
            return itens.stream().map(i -> "- " + i).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Nao foi possivel ler '{}' do analysisJson: {}", campo, e.getMessage());
            return "";
        }
    }

    /**
     * Aderência entre um currículo do usuário e uma vaga externa.
     *
     * Limitado por usuário: cada chamada consome crédito da Anthropic, e sem teto
     * alguém abrindo a lista inteira de vagas geraria dezenas de análises.
     */
    public String matchToJob(Long resumeId, String jobTitle, String jobDescription, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!rateLimiter.tryAcquire("match:user:" + user.getId(), maxMatchesPorHora, 60)) {
            throw new IllegalArgumentException(
                "Você atingiu o limite de análises de aderência por hora. Tente novamente mais tarde.");
        }

        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Currículo não encontrado"));

        if (resume.getContent() == null || resume.getContent().isBlank()) {
            throw new IllegalArgumentException("Este currículo não tem texto extraído.");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("A vaga não tem descrição suficiente para comparar.");
        }

        String json = anthropicService.matchResumeToJob(
            resume.getContent(), jobTitle == null ? "" : jobTitle, jobDescription);

        String limpo = json == null ? "" : json.strip();
        if (limpo.startsWith("```")) {
            limpo = limpo.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
        }
        return limpo;
    }

    public List<ResumeResponse> getUserResumes(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return resumeRepository.findByUserIdAndActiveTrue(user.getId())
            .stream().map(r -> toResponse(r, null)).collect(Collectors.toList());
    }

    public ResumeResponse getResume(Long id, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Resume resume = resumeRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        // O Mongo guarda a analise detalhada, mas e opcional: se estiver fora do ar,
        // esta chamada lancava excecao e a requisicao inteira virava 500 — o mesmo 500
        // que aparecia no console ao abrir um curriculo. Agora a falha e absorvida e
        // a tela se vira com o analysisJson do MySQL.
        String fullAnalysis = null;
        if (resume.getAnalysisMongoId() != null) {
            try {
                fullAnalysis = analysisRepository.findById(resume.getAnalysisMongoId())
                    .map(ResumeAnalysis::getFullAnalysis)
                    .orElse(null);
            } catch (Exception e) {
                log.warn("MongoDB indisponivel ao buscar analise do curriculo {}: {}", id, e.getMessage());
            }
        }
        return toResponse(resume, fullAnalysis);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Arquivo vazio");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("Arquivo muito grande (máx 10MB)");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Use PDF, DOCX ou TXT.");
    }

    private String extractText(MultipartFile file) {
        try {
            String type = file.getContentType();
            if ("application/pdf".equals(type)) {
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(type)) {
                try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                    return new XWPFWordExtractor(doc).getText();
                }
            } else {
                return new String(file.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Nota geral
    // ------------------------------------------------------------------

    /** Pesos da nota geral. Somam 1.0 — se mexer aqui, atualize tambem o texto da tela. */
    public static final int PESO_SKILLS      = 35;
    public static final int PESO_EXPERIENCIA = 30;
    public static final int PESO_ATS         = 20;
    public static final int PESO_FORMATO     = 15;

    /**
     * Media ponderada das quatro dimensoes.
     *
     * Skills e experiencia pesam mais porque decidem se a pessoa e adequada a vaga;
     * ATS e formato importam para o curriculo ser lido, mas nao substituem conteudo.
     * Sendo deterministico, o mesmo curriculo produz sempre a mesma nota, e o
     * calculo pode ser conferido a mao pelo candidato.
     */
    private Integer calcularNotaGeral(Resume r) {
        Integer skills = r.getSkillsScore();
        Integer exp    = r.getExperienceScore();
        Integer ats    = r.getAtsScore();
        Integer fmt    = r.getFormatScore();

        // Se a IA deixou alguma dimensao de fora, nao ha nota geral confiavel.
        if (skills == null || exp == null || ats == null || fmt == null) {
            log.warn("Nota geral nao calculada: alguma dimensao veio nula (resume {})", r.getId());
            return null;
        }

        int total = skills * PESO_SKILLS
                  + exp    * PESO_EXPERIENCIA
                  + ats    * PESO_ATS
                  + fmt    * PESO_FORMATO;

        return Math.round(total / 100f);
    }

    private ResumeResponse toResponse(Resume r, String analysis) {
        return ResumeResponse.builder()
            .id(r.getId())
            .title(r.getTitle())
            .fileType(r.getFileType())
            .overallScore(r.getOverallScore())
            .skillsScore(r.getSkillsScore())
            .experienceScore(r.getExperienceScore())
            .formatScore(r.getFormatScore())
            .atsScore(r.getAtsScore())
            .status(r.getStatus())
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .analysis(analysis)
            // Vem do MySQL, entao a tela nao depende mais do MongoDB estar de pe.
            .analysisJson(r.getAnalysisJson())
            .scoreWeights(Map.of(
                "skills", PESO_SKILLS,
                "experience", PESO_EXPERIENCIA,
                "ats", PESO_ATS,
                "format", PESO_FORMATO))
            .content(r.getContent())
            .build();
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object val) {
        if (val instanceof List<?> list) return (List<String>) list;
        return List.of();
    }
}
