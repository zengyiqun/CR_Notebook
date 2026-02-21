package com.cr.notebook.controller;

import com.cr.notebook.dto.NoteDTO;
import com.cr.notebook.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 笔记 REST 控制器。
 * 提供笔记 CRUD、全文搜索、反向链接查询和知识图谱数据接口。
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<List<NoteDTO>> list(@RequestParam(required = false) Long folderId) {
        return ResponseEntity.ok(noteService.listNotes(folderId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getNote(id));
    }

    @PostMapping
    public ResponseEntity<NoteDTO> create(@RequestBody NoteDTO dto) {
        return ResponseEntity.ok(noteService.createNote(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteDTO> update(@PathVariable Long id, @RequestBody NoteDTO dto) {
        return ResponseEntity.ok(noteService.updateNote(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<NoteDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(noteService.searchNotes(q));
    }

    /** 获取引用了指定笔记的所有反向链接（双链笔记） */
    @GetMapping("/{id}/backlinks")
    public ResponseEntity<List<NoteDTO>> backlinks(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.getBacklinks(id));
    }

    /** 获取知识图谱数据：所有笔记节点及其双链关系边 */
    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> graph() {
        return ResponseEntity.ok(noteService.getGraphData());
    }
}
