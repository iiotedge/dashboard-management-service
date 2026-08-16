package com.iotmining.services.dmss.security;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Every DashboardController endpoint takes tenantId directly as a request
 * parameter rather than deriving it from a looked-up resource (unlike
 * device-management-service/device-profile-service's evaluators, which
 * resolve tenant ownership from an entity by ID) - so the check that
 * actually matches this API's shape is a direct tenant-match against the
 * caller's own JWT claim, not a per-resource lookup.
 */
@Component("dashboardSecurity")
@Slf4j
public class DashboardSecurityEvaluator {

    public boolean canAccessTenant(Authentication authentication, String tenantId) {
        if (authentication == null || !authentication.isAuthenticated() || tenantId == null || tenantId.isBlank()) {
            return false;
        }

        if (isSuperAdmin(authentication)) {
            return true;
        }

        String userTenantId = extractClaim(authentication, "tenantId");
        if (userTenantId == null) {
            log.warn("Security Alert: Missing tenantId in token claims.");
            return false;
        }

        if (userTenantId.equals(tenantId)) {
            return true;
        }

        log.warn("Security Alert: Access denied - token tenantId {} does not match requested tenantId {}",
                userTenantId, tenantId);
        return false;
    }

    private String extractClaim(Authentication authentication, String claimKey) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Claims claims) {
            return claims.get(claimKey, String.class);
        }
        return null;
    }

    private boolean isSuperAdmin(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Claims claims) {
            return Boolean.TRUE.equals(claims.get("superAdmin", Boolean.class));
        }
        return false;
    }
}
