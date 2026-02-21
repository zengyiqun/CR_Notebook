package com.cr.notebook.integration;

import com.cr.notebook.dto.auth.AuthResponse;
import com.cr.notebook.dto.auth.LoginRequest;
import com.cr.notebook.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void fullAuthFlow_registerThenLogin() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("integration_user");
        registerReq.setEmail("integration@example.com");
        registerReq.setPassword("password123");
        registerReq.setDisplayName("Integration Tester");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("integration_user"))
                .andExpect(jsonPath("$.displayName").value("Integration Tester"))
                .andReturn();

        AuthResponse registerResp = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(registerResp.getUserId()).isNotNull();

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsernameOrEmail("integration_user");
        loginReq.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(registerResp.getUserId()));
    }

    @Test
    void register_duplicateUsername_shouldReturn400() throws Exception {
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("duplicate_user");
        req1.setEmail("dup1@example.com");
        req1.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("duplicate_user");
        req2.setEmail("dup2@example.com");
        req2.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("用户名已存在"));
    }

    @Test
    void login_wrongPassword_shouldReturn401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("nonexistent");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
