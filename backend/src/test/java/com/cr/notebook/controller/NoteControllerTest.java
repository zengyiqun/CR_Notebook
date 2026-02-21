package com.cr.notebook.controller;

import com.cr.notebook.dto.NoteDTO;
import com.cr.notebook.service.NoteService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private NoteService noteService;
    @InjectMocks private NoteController noteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(noteController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void list_shouldReturnNotes() throws Exception {
        NoteDTO note = new NoteDTO();
        note.setId(1L);
        note.setTitle("Test Note");
        note.setCreatedAt(LocalDateTime.now());
        when(noteService.listNotes(null)).thenReturn(List.of(note));

        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Note"));
    }

    @Test
    void get_shouldReturnNote() throws Exception {
        NoteDTO note = new NoteDTO();
        note.setId(1L);
        note.setTitle("Detail");
        note.setContent("Full content");
        when(noteService.getNote(1L)).thenReturn(note);

        mockMvc.perform(get("/api/notes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Detail"))
                .andExpect(jsonPath("$.content").value("Full content"));
    }

    @Test
    void create_shouldReturnCreatedNote() throws Exception {
        NoteDTO result = new NoteDTO();
        result.setId(1L);
        result.setTitle("New");
        when(noteService.createNote(any(NoteDTO.class))).thenReturn(result);

        NoteDTO input = new NoteDTO();
        input.setTitle("New");
        input.setContent("Body");

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void update_shouldReturnUpdatedNote() throws Exception {
        NoteDTO result = new NoteDTO();
        result.setId(1L);
        result.setTitle("Updated");
        when(noteService.updateNote(eq(1L), any(NoteDTO.class))).thenReturn(result);

        NoteDTO input = new NoteDTO();
        input.setTitle("Updated");

        mockMvc.perform(put("/api/notes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void search_shouldReturnResults() throws Exception {
        NoteDTO note = new NoteDTO();
        note.setId(1L);
        note.setTitle("Spring Guide");
        when(noteService.searchNotes("Spring")).thenReturn(List.of(note));

        mockMvc.perform(get("/api/notes/search").param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Spring Guide"));
    }

    @Test
    void backlinks_shouldReturnLinkedNotes() throws Exception {
        NoteDTO note = new NoteDTO();
        note.setId(2L);
        note.setTitle("Linking Note");
        when(noteService.getBacklinks(1L)).thenReturn(List.of(note));

        mockMvc.perform(get("/api/notes/1/backlinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title").value("Linking Note"));
    }

    @Test
    void graph_shouldReturnNodesAndEdges() throws Exception {
        Map<String, Object> graphData = Map.of(
                "nodes", List.of(Map.of("id", 1L, "title", "Note A")),
                "edges", List.of(Map.of("source", 1L, "target", 2L))
        );
        when(noteService.getGraphData()).thenReturn(graphData);

        mockMvc.perform(get("/api/notes/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].title").value("Note A"))
                .andExpect(jsonPath("$.edges[0].source").value(1));
    }
}
