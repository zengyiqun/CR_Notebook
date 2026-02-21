package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task extends TenantAwareEntity {

    private Long noteId;

    private String content;

    @Builder.Default
    private Boolean completed = false;

    @Builder.Default
    private String priority = "MEDIUM";

    private LocalDate dueDate;
}
