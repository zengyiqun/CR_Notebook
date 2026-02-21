package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.auth.AuthResponse;
import com.cr.notebook.dto.auth.LoginRequest;
import com.cr.notebook.dto.auth.RegisterRequest;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.UserMapper;
import com.cr.notebook.security.JwtTokenProvider;
import com.cr.notebook.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndReturnToken() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setEmail("new@example.com");
        req.setPassword("password");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser", "test@example.com", "hashed");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(principal)).thenReturn("jwt-token");

        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .build();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("testuser");
        req.setPassword("password");

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }
}
