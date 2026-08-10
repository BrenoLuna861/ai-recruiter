package com.airecruiter.exception;

import lombok.Getter;

/**
 * Login barrado por falta de confirmacao do e-mail.
 *
 * Excecao propria, e nao BadCredentialsException, porque o frontend precisa
 * distinguir os dois casos: senha errada e um erro a corrigir na mesma tela;
 * conta nao confirmada e um desvio para a tela de confirmacao. Carrega o e-mail
 * para a tela ja saber para quem reenviar o codigo.
 */
@Getter
public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Confirme seu e-mail para entrar. Enviamos um código no cadastro.");
        this.email = email;
    }
}
