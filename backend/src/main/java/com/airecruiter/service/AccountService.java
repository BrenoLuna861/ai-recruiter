package com.airecruiter.service;

import com.airecruiter.entity.Job;
import com.airecruiter.entity.User;
import com.airecruiter.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Exclusao de conta.
 *
 * A LGPD garante ao titular o direito de eliminacao dos dados pessoais (art. 18,
 * VI). Ate aqui isso so era possivel mexendo no banco a mao, o que nao escala e
 * e arriscado.
 *
 * O que apagamos: curriculos, analises, candidaturas, vagas publicadas,
 * conversas com a assistente, tokens pendentes e o proprio cadastro. Nao ha nada
 * que a plataforma seja obrigada a reter — sem emissao fiscal, sem contrato.
 *
 * O que se perde junto, e o usuario precisa saber antes: as candidaturas dele
 * somem dos rankings dos recrutadores, e as vagas publicadas por ele somem para
 * os candidatos que se inscreveram.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final ChatLogRepository chatLogRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    /**
     * @param confirmacao o proprio e-mail, digitado pelo usuario.
     *
     * Pedimos o e-mail em vez da senha porque quem entrou pelo Google nao tem
     * uma senha utilizavel — checar senha excluiria essas pessoas do direito de
     * apagar a propria conta. Digitar o endereco cumpre o mesmo papel: impede a
     * exclusao por clique acidental.
     */
    @Transactional
    public void excluirConta(String email, String confirmacao) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        if (confirmacao == null || !confirmacao.trim().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException(
                "Digite exatamente o e-mail da sua conta para confirmar a exclusão.");
        }

        Long uid = user.getId();

        // A ORDEM IMPORTA: o que referencia precisa sair antes do referenciado,
        // senao a chave estrangeira bloqueia.
        applicationRepository.deleteByCandidateId(uid);

        List<Long> idsDasVagas = jobRepository.findByRecruiterId(uid)
            .stream().map(Job::getId).toList();
        if (!idsDasVagas.isEmpty()) {
            applicationRepository.deleteByJobIdIn(idsDasVagas);
        }
        jobRepository.deleteByRecruiterId(uid);

        resumeRepository.deleteByUserId(uid);
        passwordResetTokenRepository.deleteByEmail(user.getEmail());
        emailVerificationCodeRepository.deleteByEmail(user.getEmail());

        // MongoDB e opcional na arquitetura: se estiver fora, a exclusao do que
        // esta no MySQL nao pode ser abortada por causa dele. Registramos para
        // uma limpeza posterior em vez de falhar a operacao inteira.
        try {
            chatLogRepository.deleteByUserId(uid);
            resumeAnalysisRepository.deleteByUserId(uid);
        } catch (Exception e) {
            log.error("Conta {} excluida do MySQL, mas a limpeza no MongoDB falhou: {}",
                uid, e.getMessage());
        }

        userRepository.delete(user);
        log.info("Conta excluida a pedido do titular (id={})", uid);
    }
}
