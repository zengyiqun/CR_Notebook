package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.TaskDTO;
import com.cr.notebook.entity.Task;
import com.cr.notebook.mapper.TaskMapper;
import com.cr.notebook.tenant.TenantContext;
import com.cr.notebook.tenant.TenantType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        TenantContext.set(1L, TenantType.PERSONAL);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listTasks_shouldReturnAll() {
        Task task = buildTask(1L, "Buy groceries", false);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));

        List<TaskDTO> result = taskService.listTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Buy groceries");
    }

    @Test
    void createTask_shouldInsert() {
        when(taskMapper.insert(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        TaskDTO input = new TaskDTO();
        input.setContent("New task");
        input.setPriority("HIGH");
        input.setDueDate(LocalDate.of(2026, 3, 1));

        TaskDTO result = taskService.createTask(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("New task");
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getCompleted()).isFalse();
    }

    @Test
    void updateTask_shouldModify() {
        Task existing = buildTask(1L, "Old task", false);
        when(taskMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        TaskDTO update = new TaskDTO();
        update.setCompleted(true);

        TaskDTO result = taskService.updateTask(1L, update);

        assertThat(result.getCompleted()).isTrue();
    }

    @Test
    void updateTask_notFound_shouldThrow() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.updateTask(999L, new TaskDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task not found");
    }

    @Test
    void deleteTask_shouldCallMapper() {
        when(taskMapper.deleteById(1L)).thenReturn(1);

        taskService.deleteTask(1L);

        verify(taskMapper).deleteById(1L);
    }

    private Task buildTask(Long id, String content, boolean completed) {
        Task task = Task.builder()
                .content(content)
                .completed(completed)
                .priority("MEDIUM")
                .build();
        task.setId(id);
        task.setTenantId(1L);
        task.setTenantType(TenantType.PERSONAL);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
