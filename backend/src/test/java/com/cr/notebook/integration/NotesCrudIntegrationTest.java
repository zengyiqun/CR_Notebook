package com.cr.notebook.integration;

import com.cr.notebook.dto.NoteDTO;
import com.cr.notebook.dto.auth.AuthResponse;
import com.cr.notebook.dto.auth.RegisterRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotesCrudIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String token;
    private static Long noteId;

    @Test
    @Order(1)
    void setup_registerUser() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("notes_test_user");
        req.setEmail("notes@example.com");
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        token = resp.getToken();
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    void listNotes_empty_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Order(3)
    void createNote_shouldReturnWithId() throws Exception {
        NoteDTO input = new NoteDTO();
        input.setTitle("集成测试笔记");
        input.setContent("这是通过集成测试创建的笔记");
        input.setExcerpt("集成测试...");

        MvcResult result = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("集成测试笔记"))
                .andExpect(jsonPath("$.content").value("这是通过集成测试创建的笔记"))
                .andReturn();

        NoteDTO created = objectMapper.readValue(
                result.getResponse().getContentAsString(), NoteDTO.class);
        noteId = created.getId();
        assertThat(noteId).isNotNull();
    }

    @Test
    @Order(4)
    void getNote_shouldReturnCreatedNote() throws Exception {
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("集成测试笔记"));
    }

    @Test
    @Order(5)
    void updateNote_shouldModifyTitle() throws Exception {
        NoteDTO update = new NoteDTO();
        update.setTitle("更新后的标题");

        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("更新后的标题"))
                .andExpect(jsonPath("$.content").value("这是通过集成测试创建的笔记"));
    }

    @Test
    @Order(6)
    void searchNotes_shouldFindByTitle() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "更新"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(7)
    void deleteNote_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Order(8)
    void accessWithoutToken_shouldReturn401or403() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().is4xxClientError());
    }
}
