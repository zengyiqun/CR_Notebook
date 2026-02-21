package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WhiteboardDTO {
    private Long id;
    private String title;
    private String data;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
