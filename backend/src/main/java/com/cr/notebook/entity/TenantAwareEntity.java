package com.cr.notebook.entity;

import com.cr.notebook.tenant.TenantType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class TenantAwareEntity extends BaseEntity {

    private Long tenantId;

    private TenantType tenantType;
}
