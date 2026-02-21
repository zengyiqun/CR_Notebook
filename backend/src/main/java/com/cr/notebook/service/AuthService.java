package com.cr.notebook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.auth.AuthResponse;
import com.cr.notebook.dto.auth.LoginRequest;
import com.cr.notebook.dto.auth.RegisterRequest;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.UserMapper;
import com.cr.notebook.security.JwtTokenProvider;
import com.cr.notebook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, req.getEmail())) > 0) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .displayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername())
                .build();
        userMapper.insert(user);

        UserPrincipal principal = UserPrincipal.from(user);
        String token = tokenProvider.generateToken(principal);

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = tokenProvider.generateToken(principal);

        User user = userMapper.selectById(principal.getId());
        if (user == null) throw new IllegalArgumentException("用户不存在");
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
    }
}
