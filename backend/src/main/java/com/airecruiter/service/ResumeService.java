package com.airecruiter.service;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

            resume.setOverallScore(toInt(data.get("overallScore")));
            resume.setSkillsScore(toInt(data.get("skillsScore")));
            resume.setExperienceScore(toInt(data.get("experienceScore")));
            resume.setFormatScore(toInt(data.get("formatScore")));
            resume.setAtsScore(toInt(data.get("atsScore")));
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

        String fullAnalysis = null;
        if (resume.getAnalysisMongoId() != null) {
            fullAnalysis = analysisRepository.findById(resume.getAnalysisMongoId())
                .map(ResumeAnalysis::getFullAnalysis)
                .orElse(null);
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