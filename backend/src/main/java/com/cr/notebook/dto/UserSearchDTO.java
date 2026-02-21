package com.cr.notebook.dto;

import lombok.Data;

@Data
public class UserSearchDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
}
