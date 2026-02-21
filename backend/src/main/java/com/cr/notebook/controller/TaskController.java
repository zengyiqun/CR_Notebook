package com.cr.notebook.controller;

import com.cr.notebook.dto.TaskDTO;
import com.cr.notebook.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDTO>> list() {
        return ResponseEntity.ok(taskService.listTasks());
    }

    @PostMapping
    public ResponseEntity<TaskDTO> create(@Valid @RequestBody TaskDTO dto) {
        return ResponseEntity.ok(taskService.createTask(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> update(@PathVariable Long id, @RequestBody TaskDTO dto) {
        return ResponseEntity.ok(taskService.updateTask(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
