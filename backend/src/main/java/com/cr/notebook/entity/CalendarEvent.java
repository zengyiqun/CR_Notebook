package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@TableName("calendar_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarEvent extends TenantAwareEntity {

    private String title;

    private LocalDate eventDate;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime eventTime;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate endDate;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalTime endTime;

    private String description;

    @Builder.Default
    private String color = "#6366f1";
}
