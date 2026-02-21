package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("sys_org_member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgMember extends BaseEntity {

    private Long organizationId;

    private Long userId;

    @Builder.Default
    private String role = "MEMBER";
}
