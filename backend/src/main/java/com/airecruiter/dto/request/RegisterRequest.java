package com.airecruiter.dto.request;

import com.airecruiter.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 2, max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 191)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    private User.Role role = User.Role.CANDIDATE;
}
