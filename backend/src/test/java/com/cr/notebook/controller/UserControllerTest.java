package com.cr.notebook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock private UserMapper userMapper;
    @InjectMocks private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void searchUsers_withValidQuery_shouldReturnResults() throws Exception {
        User u1 = User.builder().username("alice").email("alice@example.com").displayName("Alice").passwordHash("h").build();
        u1.setId(1L);
        User u2 = User.builder().username("alex").email("alex@example.com").displayName("Alex").passwordHash("h").build();
        u2.setId(2L);

        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users/search").param("q", "al"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[1].username").value("alex"));
    }

    @Test
    void searchUsers_queryTooShort_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/users/search").param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchUsers_noResults_shouldReturnEmpty() throws Exception {
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/users/search").param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchUsers_shouldNotExposePasswordHash() throws Exception {
        User u = User.builder().username("bob").email("bob@example.com").displayName("Bob").passwordHash("secret").build();
        u.setId(1L);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users/search").param("q", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
