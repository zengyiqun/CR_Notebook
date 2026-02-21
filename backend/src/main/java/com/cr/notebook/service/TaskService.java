package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.TaskDTO;
import com.cr.notebook.entity.Task;
import com.cr.notebook.mapper.TaskMapper;
import com.cr.notebook.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    public List<TaskDTO> listTasks() {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getTenantId, TenantContext.getTenantId())
                        .eq(Task::getTenantType, TenantContext.getTenantType())
                        .orderByDesc(Task::getCreatedAt));
        return tasks.stream().map(this::toDTO).toList();
    }

    @Transactional
    public TaskDTO createTask(TaskDTO dto) {
        Task task = Task.builder()
                .noteId(dto.getNoteId())
                .content(dto.getContent())
                .completed(dto.getCompleted() != null && dto.getCompleted())
                .priority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM")
                .dueDate(dto.getDueDate())
                .build();
        task.setTenantId(TenantContext.getTenantId());
        task.setTenantType(TenantContext.getTenantType());
        taskMapper.insert(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new IllegalArgumentException("Task not found");
        if (dto.getContent() != null) task.setContent(dto.getContent());
        if (dto.getCompleted() != null) task.setCompleted(dto.getCompleted());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getDueDate() != null) task.setDueDate(dto.getDueDate());
        taskMapper.updateById(task);
        return toDTO(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskMapper.deleteById(id);
    }

    private TaskDTO toDTO(Task t) {
        TaskDTO dto = new TaskDTO();
        dto.setId(t.getId());
        dto.setNoteId(t.getNoteId());
        dto.setContent(t.getContent());
        dto.setCompleted(t.getCompleted());
        dto.setPriority(t.getPriority());
        dto.setDueDate(t.getDueDate());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
