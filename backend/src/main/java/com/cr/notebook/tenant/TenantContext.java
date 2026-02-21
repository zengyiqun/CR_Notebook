package com.cr.notebook.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<TenantType> TENANT_TYPE = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId, TenantType tenantType) {
        TENANT_ID.set(tenantId);
        TENANT_TYPE.set(tenantType);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static TenantType getTenantType() {
        return TENANT_TYPE.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_TYPE.remove();
    }
}
