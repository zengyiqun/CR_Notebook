package com.cr.notebook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("whiteboard")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Whiteboard extends TenantAwareEntity {

    @Builder.Default
    private String title = "新白板";

    private String data;
}
