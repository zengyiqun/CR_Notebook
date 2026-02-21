package com.cr.notebook.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;

    public AuthResponse(String token, Long userId, String username, String email, String displayName, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }
}
