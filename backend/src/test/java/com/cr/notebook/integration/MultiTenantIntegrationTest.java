package com.cr.notebook.integration;

import com.cr.notebook.dto.FolderDTO;
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
class MultiTenantIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String tokenA;
    private static String tokenB;

    @Test
    @Order(1)
    void setup_registerTwoUsers() throws Exception {
        tokenA = registerAndGetToken("tenant_user_a", "tenantA@example.com");
        tokenB = registerAndGetToken("tenant_user_b", "tenantB@example.com");
        assertThat(tokenA).isNotBlank();
        assertThat(tokenB).isNotBlank();
    }

    @Test
    @Order(2)
    void userA_createFolder_shouldSucceed() throws Exception {
        FolderDTO input = new FolderDTO();
        input.setName("User A's Folder");

        mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User A's Folder"));
    }

    @Test
    @Order(3)
    void userB_createFolder_shouldSucceed() throws Exception {
        FolderDTO input = new FolderDTO();
        input.setName("User B's Folder");

        mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User B's Folder"));
    }

    @Test
    @Order(4)
    void userA_shouldOnlySeeOwnFolders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/folders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        List<FolderDTO> folders = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(folders).hasSize(1);
        assertThat(folders.get(0).getName()).isEqualTo("User A's Folder");
    }

    @Test
    @Order(5)
    void userB_shouldOnlySeeOwnFolders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/folders")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        List<FolderDTO> folders = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(folders).hasSize(1);
        assertThat(folders.get(0).getName()).isEqualTo("User B's Folder");
    }

    private String registerAndGetToken(String username, String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return resp.getToken();
    }
}
