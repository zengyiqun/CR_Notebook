package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

@TableName(value = "note", autoResultMap = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Note extends TenantAwareEntity {

    private Long folderId;

    @Builder.Default
    private String title = "";

    private String content;

    @Builder.Default
    private String excerpt = "";

    @Builder.Default
    private Boolean isPinned = false;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
}
