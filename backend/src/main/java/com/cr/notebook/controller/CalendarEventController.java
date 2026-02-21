package com.cr.notebook.controller;

import com.cr.notebook.dto.CalendarEventDTO;
import com.cr.notebook.service.CalendarEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar-events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping
    public ResponseEntity<List<CalendarEventDTO>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(calendarEventService.listEvents(startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<CalendarEventDTO> create(@RequestBody CalendarEventDTO dto) {
        return ResponseEntity.ok(calendarEventService.createEvent(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CalendarEventDTO> update(@PathVariable Long id, @RequestBody CalendarEventDTO dto) {
        return ResponseEntity.ok(calendarEventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
