package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("daily_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyNote extends TenantAwareEntity {

    private LocalDate noteDate;

    private String content;

    private String weather;

    private String mood;
}
