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
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        // Check for explicit tenant header override (for org spaces)
        String headerTenantId = request.getHeader("X-Tenant-Id");
        String headerTenantType = request.getHeader("X-Tenant-Type");

        if (headerTenantId != null && headerTenantType != null) {
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
        TenantContext.clear();
    }
}
