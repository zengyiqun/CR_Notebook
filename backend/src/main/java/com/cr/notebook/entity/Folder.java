package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("note_folder")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Folder extends TenantAwareEntity {

    private String name;

    @Builder.Default
    private String icon = "📁";

    private Long parentId;

    @Builder.Default
    private Integer sortOrder = 0;
}
