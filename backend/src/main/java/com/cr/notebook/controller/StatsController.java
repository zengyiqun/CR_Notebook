package com.cr.notebook.controller;

import com.cr.notebook.dto.StatsDTO;
import com.cr.notebook.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/personal")
    public ResponseEntity<StatsDTO> getPersonalStats(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statsService.getPersonalStats(days));
    }

    @GetMapping("/org/{orgId}")
    public ResponseEntity<StatsDTO> getOrgStats(@PathVariable Long orgId, @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(statsService.getOrgStats(orgId, days));
    }
}
