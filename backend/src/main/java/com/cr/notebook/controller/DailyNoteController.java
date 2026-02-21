package com.cr.notebook.controller;

import com.cr.notebook.dto.DailyNoteDTO;
import com.cr.notebook.service.DailyNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily-notes")
@RequiredArgsConstructor
public class DailyNoteController {

    private final DailyNoteService dailyNoteService;

    @GetMapping("/{date}")
    public ResponseEntity<DailyNoteDTO> get(@PathVariable LocalDate date) {
        return ResponseEntity.ok(dailyNoteService.getDailyNote(date));
    }

    @PutMapping("/{date}")
    public ResponseEntity<DailyNoteDTO> update(@PathVariable LocalDate date, @RequestBody DailyNoteDTO dto) {
        return ResponseEntity.ok(dailyNoteService.updateDailyNote(date, dto));
    }

    @GetMapping("/dates")
    public ResponseEntity<List<String>> listDatesWithContent(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(dailyNoteService.listDatesWithContent(from, to));
    }
}
