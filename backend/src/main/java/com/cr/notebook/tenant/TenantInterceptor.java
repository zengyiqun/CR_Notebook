package com.cr.notebook.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 未认证请求不写入 TenantContext，交给后续鉴权链路处理。
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        // Check for explicit tenant header override (for org spaces)
        String headerTenantId = request.getHeader("X-Tenant-Id");
        String headerTenantType = request.getHeader("X-Tenant-Type");

        if (headerTenantId != null && headerTenantType != null) {
            // 前端显式传入组织上下文时，按请求头切换租户。
            TenantContext.set(Long.parseLong(headerTenantId), TenantType.valueOf(headerTenantType));
        } else {
            // Default: personal space = user id
            Long userId = ((com.cr.notebook.security.UserPrincipal) auth.getPrincipal()).getId();
            TenantContext.set(userId, TenantType.PERSONAL);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须在请求结束后清理 ThreadLocal，避免线程复用导致租户串线。
        TenantContext.clear();
    }
}
