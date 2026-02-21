package com.cr.notebook.controller;

import com.cr.notebook.dto.TaskDTO;
import com.cr.notebook.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private TaskService taskService;
    @InjectMocks private TaskController taskController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void list_shouldReturnTasks() throws Exception {
        TaskDTO task = new TaskDTO();
        task.setId(1L);
        task.setContent("Buy milk");
        task.setCompleted(false);
        task.setCreatedAt(LocalDateTime.now());
        when(taskService.listTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Buy milk"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        TaskDTO result = new TaskDTO();
        result.setId(1L);
        result.setContent("New task");
        result.setCompleted(false);
        when(taskService.createTask(any(TaskDTO.class))).thenReturn(result);

        TaskDTO input = new TaskDTO();
        input.setContent("New task");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("New task"));
    }

    @Test
    void update_shouldReturnUpdated() throws Exception {
        TaskDTO result = new TaskDTO();
        result.setId(1L);
        result.setContent("Updated");
        result.setCompleted(true);
        when(taskService.updateTask(eq(1L), any(TaskDTO.class))).thenReturn(result);

        TaskDTO input = new TaskDTO();
        input.setCompleted(true);
        input.setContent("Updated");

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
    }
}
