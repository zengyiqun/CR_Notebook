package com.cr.notebook.controller;

import com.cr.notebook.dto.WhiteboardDTO;
import com.cr.notebook.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whiteboards")
@RequiredArgsConstructor
public class WhiteboardController {

    private final WhiteboardService whiteboardService;

    @GetMapping
    public ResponseEntity<List<WhiteboardDTO>> list() {
        return ResponseEntity.ok(whiteboardService.listWhiteboards());
    }

    @PostMapping
    public ResponseEntity<WhiteboardDTO> create(@RequestBody WhiteboardDTO dto) {
        return ResponseEntity.ok(whiteboardService.createWhiteboard(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WhiteboardDTO> update(@PathVariable Long id, @RequestBody WhiteboardDTO dto) {
        return ResponseEntity.ok(whiteboardService.updateWhiteboard(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        whiteboardService.deleteWhiteboard(id);
        return ResponseEntity.noContent().build();
    }
}
