package com.airecruiter.dto.response;

import com.airecruiter.entity.User;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private UserInfo user;

    @Data @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private User.Role role;
    }
}
