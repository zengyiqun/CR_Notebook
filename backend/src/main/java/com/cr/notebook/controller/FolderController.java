package com.cr.notebook.controller;

import com.cr.notebook.dto.FolderDTO;
import com.cr.notebook.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public ResponseEntity<List<FolderDTO>> list() {
        return ResponseEntity.ok(folderService.listFolders());
    }

    @PostMapping
    public ResponseEntity<FolderDTO> create(@Valid @RequestBody FolderDTO dto) {
        return ResponseEntity.ok(folderService.createFolder(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderDTO> update(@PathVariable Long id, @RequestBody FolderDTO dto) {
        return ResponseEntity.ok(folderService.updateFolder(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
