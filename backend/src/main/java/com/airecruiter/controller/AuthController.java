package com.airecruiter.controller;

import com.airecruiter.dto.request.GoogleAuthRequest;
import com.airecruiter.dto.request.LoginRequest;
import com.airecruiter.dto.request.RegisterRequest;
import com.airecruiter.dto.response.AuthResponse;
import com.airecruiter.service.AuthService;
import com.airecruiter.service.GoogleAuthService;
import com.airecruiter.service.EmailVerificationService;
import com.airecruiter.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    /** Cria a conta e envia o código. Não devolve token: falta confirmar. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req,
                                                        HttpServletRequest http) {
        authService.register(req, clientIp(http));
        return ResponseEntity.status(201).body(Map.of(
                "message", "Cadastro criado. Enviamos um código de confirmação para o seu e-mail.",
                "email", req.getEmail()
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        boolean ok = emailVerificationService.confirmar(req.getEmail(), req.getCode());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "message", "Código inválido ou expirado. Peça um novo."
            ));
        }
        return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "Conta confirmada. Você já pode entrar."
        ));
    }

    /** Reenvio. Responde 200 sempre, para não revelar quais e-mails têm conta. */
    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(@Valid @RequestBody ForgotPasswordRequest req,
                                                          HttpServletRequest http) {
        emailVerificationService.enviarCodigo(req.getEmail(), clientIp(http));
        return ResponseEntity.ok(Map.of("message", "Se houver cadastro pendente, um novo código foi enviado."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(googleAuthService.authenticate(req));
    }

    /**
     * Responde 200 SEMPRE, independente de o e-mail existir, de a conta estar
     * ativa ou de o envio ter falhado. Qualquer resposta diferente permitiria
     * descobrir quais e-mails tem conta na plataforma.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                                              HttpServletRequest http) {
        passwordResetService.requestPasswordReset(req.getEmail(), clientIp(http));
        return ResponseEntity.ok(Map.of("message", "Se o email existir, você receberá as instruções em breve."));
    }

    /** A tela de reset chama isto antes de mostrar o formulario. */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", passwordResetService.isTokenValid(token)));
    }

    /** Erros de negocio sobem como PasswordResetException e viram 400 no GlobalExceptionHandler. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    /**
     * Atras do proxy do Railway o remoteAddr e sempre o IP interno do balanceador,
     * o que faria o rate limit valer para o mundo inteiro de uma vez. O IP real
     * vem no X-Forwarded-For (primeiro item da lista).
     */
    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ai-recruiter"));
    }

    // DTOs internos
    @Data
    static class ForgotPasswordRequest {
        @Email @NotBlank
        private String email;
    }

    @Data
    static class VerifyEmailRequest {
        @Email @NotBlank
        private String email;
        @NotBlank
        @Size(min = 6, max = 6, message = "O código tem 6 dígitos")
        private String code;
    }

    @Data
    static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank @Size(min = 8)
        private String newPassword;
    }
}

