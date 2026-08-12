package com.airecruiter.controller;

import com.airecruiter.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Exclusão definitiva da conta do usuário autenticado.
     *
     * O id vem do token, nunca do corpo da requisição — ninguém apaga a conta de
     * outra pessoa informando um id.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> excluirConta(
            @Valid @RequestBody ExcluirContaRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        accountService.excluirConta(userDetails.getUsername(), req.getConfirmacao());
        return ResponseEntity.ok(Map.of("message", "Conta excluída."));
    }

    @Data
    static class ExcluirContaRequest {
        @NotBlank
        private String confirmacao;
    }
}
