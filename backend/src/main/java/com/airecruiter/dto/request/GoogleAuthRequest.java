package com.airecruiter.dto.request;

import com.airecruiter.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    /** ID Token (JWT) emitido pelo Google Identity Services no front. */
    @NotBlank
    private String credential;

    /** Opcional — usado quando é o primeiro login (cadastro). Default: CANDIDATE. */
    private User.Role role;
}
