package com.cr.notebook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.dto.UserSearchDTO;
import com.cr.notebook.dto.auth.ChangePasswordRequest;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.UserMapper;
import com.cr.notebook.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDTO>> searchUsers(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        String keyword = q.trim();
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getEmail, keyword)
                        .or()
                        .like(User::getDisplayName, keyword)
                        .last("LIMIT 20"));

        List<UserSearchDTO> result = users.stream().map(u -> {
            UserSearchDTO dto = new UserSearchDTO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setDisplayName(u.getDisplayName());
            dto.setAvatarUrl(u.getAvatarUrl());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSearchDTO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userMapper.selectById(principal.getId());
        if (user == null) return ResponseEntity.notFound().build();
        UserSearchDTO dto = new UserSearchDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setAvatarUrl(user.getAvatarUrl());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        User user = userMapper.selectById(principal.getId());
        if (user == null) return ResponseEntity.notFound().build();
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl != null ? avatarUrl : ""));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = userMapper.selectById(principal.getId());
        if (user == null) return ResponseEntity.notFound().build();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "原密码不正确"));
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "新密码不能与原密码相同"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }
}
