package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("sys_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    private String username;

    private String email;

    private String passwordHash;

    private String displayName;

    private String avatarUrl;
}
