package com.cr.notebook.controller;

import com.cr.notebook.dto.FolderDTO;
import com.cr.notebook.service.FolderService;
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
class FolderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private FolderService folderService;
    @InjectMocks private FolderController folderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(folderController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void list_shouldReturnFolders() throws Exception {
        FolderDTO folder = new FolderDTO();
        folder.setId(1L);
        folder.setName("Work");
        folder.setIcon("📁");
        folder.setCreatedAt(LocalDateTime.now());
        when(folderService.listFolders()).thenReturn(List.of(folder));

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Work"));
    }

    @Test
    void create_shouldReturnCreated() throws Exception {
        FolderDTO result = new FolderDTO();
        result.setId(1L);
        result.setName("New Folder");
        result.setIcon("📁");
        when(folderService.createFolder(any(FolderDTO.class))).thenReturn(result);

        FolderDTO input = new FolderDTO();
        input.setName("New Folder");

        mockMvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Folder"));
    }

    @Test
    void update_shouldReturnUpdated() throws Exception {
        FolderDTO result = new FolderDTO();
        result.setId(1L);
        result.setName("Renamed");
        when(folderService.updateFolder(eq(1L), any(FolderDTO.class))).thenReturn(result);

        FolderDTO input = new FolderDTO();
        input.setName("Renamed");

        mockMvc.perform(put("/api/folders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/folders/1"))
                .andExpect(status().isNoContent());
    }
}
