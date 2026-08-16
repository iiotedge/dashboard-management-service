package com.iotmining.services.dmss.controller;

import com.iotmining.services.dmss.dto.DashboardDto;
import com.iotmining.services.dmss.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {
    private final DashboardService dashboardService;

    @PostMapping("/create")
    @PreAuthorize("@dashboardSecurity.canAccessTenant(authentication, #tenantId)")
    public ResponseEntity<?> createDashboard(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestBody DashboardDto dto
    ) {
        log.info("[POST /api/dashboards/create] tenantId={}, title={}", tenantId, dto.getTitle());
        try {
            Map<String, Object> result = dashboardService.createDashboard(tenantId, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create dashboard for tenantId={}, title={}: {}", tenantId, dto.getTitle(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/save")
    @PreAuthorize("@dashboardSecurity.canAccessTenant(authentication, #tenantId)")
    public ResponseEntity<?> saveDashboard(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "dashboardId", required = false) String dashboardId, // allow missing dashboardId for new
            @RequestBody String dashboardJson
    ) {
        log.info("[POST /api/dashboards/save] tenantId={}, dashboardId={}", tenantId, dashboardId);
        try {
            Map<String, Object> result = dashboardService.saveDashboard(tenantId, dashboardId, dashboardJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to save dashboard for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/get")
    @PreAuthorize("@dashboardSecurity.canAccessTenant(authentication, #tenantId)")
    public ResponseEntity<?> getDashboard(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "dashboardId") String dashboardId
    ) {
        log.info("[GET /api/dashboards/get] tenantId={}, dashboardId={}", tenantId, dashboardId);
        try {
            String dashboardJson = dashboardService.getDashboard(tenantId, dashboardId);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(dashboardJson);
        } catch (Exception e) {
            log.error("Failed to fetch dashboard for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/list")
    @PreAuthorize("@dashboardSecurity.canAccessTenant(authentication, #tenantId)")
    public ResponseEntity<?> listDashboardsForTenant(@RequestParam(name = "tenantId") String tenantId) {
        List<DashboardDto> dashboards = dashboardService.listDashboardsForTenant(tenantId);
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/presigned")
    @PreAuthorize("@dashboardSecurity.canAccessTenant(authentication, #tenantId)")
    public ResponseEntity<?> getDashboardPresignedUrl(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "dashboardId") String dashboardId,
            @RequestParam(name = "expiry", defaultValue = "600") int expiry
    ) {
        log.info("[GET /api/dashboards/presigned] tenantId={}, dashboardId={}, expiry={}", tenantId, dashboardId, expiry);
        try {
            String url = dashboardService.getDashboardPresignedUrl(tenantId, dashboardId, expiry);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Failed to get presigned URL for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

