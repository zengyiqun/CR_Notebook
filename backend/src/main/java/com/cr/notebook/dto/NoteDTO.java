package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteDTO {
    private Long id;
    private Long folderId;
    private String title;
    private String content;
    private String excerpt;
    private Boolean isPinned;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
