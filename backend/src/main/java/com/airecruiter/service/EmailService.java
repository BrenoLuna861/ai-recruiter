package com.airecruiter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from:AI Recruiter <onboarding@resend.dev>}")
    private String fromEmail;

    @Value("${app.frontend-url:https://ai-recruiter-production-f3a0.up.railway.app}")
    private String frontendUrl;

    /** Injetado pelo @RequiredArgsConstructor — usado para saber se estamos em dev. */
    private final Environment environment;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    public void sendPasswordResetEmail(String toEmail, String token, int ttlMinutes) {
        // O token vai na URL, entao precisa ser URL-safe. Ele ja e base64url,
        // mas encodamos por seguranca caso o gerador mude no futuro.
        String resetLink = frontendUrl + "/reset-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        String validade = ttlMinutes >= 60
                ? (ttlMinutes / 60) + (ttlMinutes / 60 == 1 ? " hora" : " horas")
                : ttlMinutes + " minutos";

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0a0a0b; color: #e5e5e5; margin: 0; padding: 0; }
                .container { max-width: 480px; margin: 40px auto; background: #111113; border: 1px solid #222226; border-radius: 12px; overflow: hidden; }
                .header { padding: 32px 40px 24px; border-bottom: 1px solid #222226; }
                .brand { display: flex; align-items: center; gap: 8px; margin-bottom: 0; }
                .brand-mark { color: #6ee7b7; font-size: 18px; }
                .brand-text { font-size: 16px; font-weight: 600; color: #e5e5e5; }
                .body { padding: 32px 40px; }
                h1 { font-size: 22px; font-weight: 700; margin: 0 0 12px; color: #fff; }
                p { font-size: 14px; line-height: 1.7; color: #9ca3af; margin: 0 0 20px; }
                .btn { display: inline-block; background: #6ee7b7; color: #0a0a0b; font-weight: 700; font-size: 14px; padding: 14px 32px; border-radius: 8px; text-decoration: none; letter-spacing: 0.04em; }
                .btn-wrap { margin: 28px 0; }
                .link { font-size: 12px; color: #6b7280; word-break: break-all; }
                .footer { padding: 20px 40px; border-top: 1px solid #222226; font-size: 12px; color: #4b5563; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="brand">
                    <span class="brand-mark">✦</span>
                    <span class="brand-text">AI Recruiter</span>
                  </div>
                </div>
                <div class="body">
                  <h1>Recuperação de senha</h1>
                  <p>Recebemos uma solicitação para redefinir a senha da sua conta. Clique no botão abaixo para criar uma nova senha.</p>
                  <div class="btn-wrap">
                    <a href="%s" class="btn">Redefinir senha</a>
                  </div>
                  <p>Se o botão não funcionar, copie e cole este link no seu navegador:</p>
                  <p class="link">%s</p>
                  <p style="margin-top:24px">Este link expira em <strong style="color:#e5e5e5">%s</strong> e só pode ser usado uma vez. Se você não solicitou a recuperação de senha, ignore este email — sua senha atual continua valendo.</p>
                </div>
                <div class="footer">
                  AI Recruiter — Plataforma Inteligente de Recrutamento<br>
                  Este é um email automático, não responda.
                </div>
              </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, validade);

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", new String[]{toEmail},
                "subject", "Recuperação de senha — AI Recruiter",
                "html", htmlBody
        );

        if (resendApiKey == null || resendApiKey.isBlank()) {
            // Em dev, imprimir o link no log deixa o fluxo testavel sem provedor
            // nenhum. Em producao isso seria um vazamento: qualquer um com acesso
            // aos logs poderia trocar a senha de qualquer conta. Por isso o link
            // so aparece no profile dev.
            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                log.warn("""
                        RESEND_API_KEY nao configurada — e-mail NAO foi enviado.
                        Link de recuperacao (valido por {} min): {}""",
                        ttlMinutes, resetLink);
            } else {
                log.error("RESEND_API_KEY nao configurada — nenhum e-mail de recuperacao "
                        + "esta sendo enviado. Configure a variavel no ambiente.");
            }
            return;
        }

        try {
            // block() com timeout: sem ele, uma instabilidade do Resend prenderia a
            // thread da requisicao indefinidamente.
            webClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientResponseException e) {
            // O status sozinho nao diz nada util: o Resend explica a recusa no corpo
            // da resposta ("domain not verified", "can only send to your own
            // address", etc). Sem isso, um 403 vira adivinhacao.
            throw new IllegalStateException(
                    "Resend recusou o envio (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }

        log.info("Email de recuperação enviado");
    }
}
