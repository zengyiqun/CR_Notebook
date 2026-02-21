package com.cr.notebook.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String description;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
