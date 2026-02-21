package com.cr.notebook.controller;

import com.cr.notebook.dto.auth.AuthResponse;
import com.cr.notebook.dto.auth.LoginRequest;
import com.cr.notebook.dto.auth.RegisterRequest;
import com.cr.notebook.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_shouldReturnToken() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", 1L, "testuser", "test@example.com", "Test User", null);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", 1L, "testuser", "test@example.com", "Test User", null);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("testuser");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(1));
    }
}
