package com.airecruiter.exception;

/**
 * Erro de negocio do fluxo de recuperacao de senha.
 *
 * A mensagem para os casos de token e propositalmente generica ("Link invalido
 * ou expirado"): distinguir "nao existe" de "expirou" de "ja foi usado" da ao
 * atacante um oraculo para sondar tokens. O motivo real vai para o log.
 */
public class PasswordResetException extends RuntimeException {

    public PasswordResetException(String message) {
        super(message);
    }

    public static PasswordResetException invalidToken() {
        return new PasswordResetException("Link invalido ou expirado. Solicite um novo.");
    }
}
