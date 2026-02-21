package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrgMemberDTO {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String role;
    private LocalDateTime joinedAt;
}
