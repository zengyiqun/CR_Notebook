package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrganizationDTO {
    private Long id;
    private String name;
    private Long ownerId;
    private String ownerName;
    private String role;
    private LocalDateTime createdAt;
    private int memberCount;
    private String avatarUrl;
}
