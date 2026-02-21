package com.cr.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FolderDTO {
    private Long id;

    @NotBlank
    private String name;

    private String icon = "📁";
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
