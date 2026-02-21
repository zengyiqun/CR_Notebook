package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("sys_organization")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization extends BaseEntity {

    private String name;

    private Long ownerId;

    private String avatarUrl;
}
