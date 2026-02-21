package com.cr.notebook.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cr.notebook.entity.User;
import com.cr.notebook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, usernameOrEmail)
                        .or()
                        .eq(User::getEmail, usernameOrEmail));
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + usernameOrEmail);
        }
        return UserPrincipal.from(user);
    }

    public UserDetails loadUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        return UserPrincipal.from(user);
    }
}
