package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyNoteDTO {
    private Long id;
    private LocalDate noteDate;
    private String content;
    private String weather;
    private String mood;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
