package com.cr.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaskDTO {
    private Long id;
    private Long noteId;

    @NotBlank
    private String content;

    private Boolean completed = false;
    private String priority = "MEDIUM";
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
