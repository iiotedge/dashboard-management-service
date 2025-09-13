package com.iotmining.services.dmss.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iotmining.services.dmss.client.ObjectStorageClient;
import com.iotmining.services.dmss.dto.DashboardDto;
import com.iotmining.services.dmss.model.DashboardMetadata;
import com.iotmining.services.dmss.repository.DashboardMetadataRepository;
import com.iotmining.services.dmss.util.CustomMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ObjectStorageClient objectStorageClient;
    private final DashboardMetadataRepository metadataRepository;

    private static final String DASHBOARD_OBJECT_TYPE = "dashboard";
    private static final String DASHBOARD_CATEGORY = "UI";

    public Map<String, Object> createDashboard(String tenantId, DashboardDto dto) {
        String dashboardId = dto.getId() != null ? dto.getId() : UUID.randomUUID().toString();

        String objectUrl = String.format("%s/%s/UI/dashboard/%s.json", tenantId, dashboardId, dashboardId);

        DashboardMetadata meta = new DashboardMetadata();
        meta.setId(dashboardId);
        meta.setTenantId(tenantId);
        meta.setTitle(dto.getTitle());
        meta.setCreatedTime(dto.getCreatedTime());
        meta.setAssignedCustomers(dto.getAssignedCustomers());
        meta.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
        meta.setVersion(dto.getVersion());
        meta.setObjectUrl(objectUrl); // Predefined location for dashboard JSON file
        meta.setLastUpdatedTime(meta.getCreatedTime());

        metadataRepository.save(meta);

        return Map.of(
                "dashboardId", dashboardId,
                "objectUrl", objectUrl,
                "createdTime", meta.getCreatedTime()
        );
    }
    public Map<String, Object> saveDashboard(String tenantId, String dashboardId, String dashboardJson) throws Exception {
        DashboardMetadata meta = metadataRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new RuntimeException("Dashboard not found!"));

        MultipartFile file = new CustomMultipartFile(
                dashboardId + ".json",
                dashboardId + ".json",
                "application/json",
                dashboardJson.getBytes(StandardCharsets.UTF_8)
        );
        Map<String, Object> result = objectStorageClient.uploadSimple(
                file, tenantId, dashboardId, DASHBOARD_OBJECT_TYPE, DASHBOARD_CATEGORY, 86400);

        meta.setObjectUrl((String) result.getOrDefault("objectUrl", meta.getObjectUrl()));
        meta.setObjectSize(file.getSize());
        meta.setFileName(file.getOriginalFilename());
        meta.setLastUpdatedTime(java.time.OffsetDateTime.now().toString());

        metadataRepository.save(meta);

        return Map.of(
                "dashboardId", dashboardId,
                "objectUrl", meta.getObjectUrl(),
                "objectSize", meta.getObjectSize(),
                "fileName", meta.getFileName(),
                "lastUpdatedTime", meta.getLastUpdatedTime()
        );
    }

//    public Map<String, Object> saveDashboard(
//            String tenantId, String dashboardId, String dashboardJson) throws Exception {
//
//        String finalDashboardId = (dashboardId == null || dashboardId.isBlank())
//                ? UUID.randomUUID().toString()
//                : dashboardId;
//
//        log.info("[saveDashboard] Called with tenantId={}, dashboardId={}, payloadSize={} bytes",
//                tenantId, finalDashboardId, dashboardJson != null ? dashboardJson.length() : 0);
//
//        MultipartFile file = new CustomMultipartFile(
//                finalDashboardId + ".json",
//                finalDashboardId + ".json",
//                "application/json",
//                dashboardJson.getBytes(StandardCharsets.UTF_8)
//        );
//        log.debug("Created CustomMultipartFile for dashboard '{}', filename='{}'", finalDashboardId, finalDashboardId + ".json");
//
//        // Upload to Minio/object storage
//        Map<String, Object> result = objectStorageClient.uploadSimple(
//                file,
//                tenantId,
//                finalDashboardId,
//                DASHBOARD_OBJECT_TYPE,
//                DASHBOARD_CATEGORY,
//                86400
//        );
//
//        ObjectMapper mapper = new ObjectMapper();
//        DashboardDto dto = mapper.readValue(dashboardJson, DashboardDto.class);
//        String nowIso = OffsetDateTime.now().toString();
//
//        // Save metadata to Postgres
//        DashboardMetadata meta = new DashboardMetadata();
//        meta.setId(finalDashboardId);
//        meta.setTenantId(tenantId);
//        meta.setTitle(dto.getTitle());
//        meta.setCreatedTime(dto.getCreatedTime() != null ? dto.getCreatedTime() : nowIso);
//        meta.setAssignedCustomers(dto.getAssignedCustomers());
//        meta.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
//        meta.setVersion(dto.getVersion());
//        meta.setObjectUrl((String) result.getOrDefault("objectUrl", String.format("%s/%s/%s/%s/%s.json", tenantId, finalDashboardId, DASHBOARD_CATEGORY, DASHBOARD_OBJECT_TYPE, finalDashboardId)));
//        meta.setObjectSize(file.getSize());
//        meta.setFileName(file.getOriginalFilename());
//        meta.setLastUpdatedTime(nowIso);
//
//        metadataRepository.save(meta);
//
//        Map<String, Object> response = new HashMap<>(result);
//        response.put("dashboardId", finalDashboardId);
//        response.put("objectUrl", meta.getObjectUrl());
//        response.put("objectSize", meta.getObjectSize());
//        response.put("fileName", meta.getFileName());
//        response.put("lastUpdatedTime", meta.getLastUpdatedTime());
//        log.info("Dashboard uploaded and metadata saved for tenantId={}, dashboardId={}", tenantId, finalDashboardId);
//        return response;
//    }

//    public String getDashboard(
//            String tenantId, String dashboardId) throws Exception {
//        log.info("[getDashboard] Fetching for tenantId={}, dashboardId={}", tenantId, dashboardId);
//        try {
//            DashboardMetadata meta = metadataRepository.findByIdAndTenantId(dashboardId, tenantId)
//                    .orElseThrow(() -> new RuntimeException("Dashboard metadata not found!"));
//            String objectUrl = meta.getObjectUrl();
//            if (objectUrl == null || objectUrl.isEmpty())
//                throw new RuntimeException("Dashboard object URL missing in metadata");
//
//            // Only use object storage for download
//            org.springframework.core.io.Resource resource =
//                    objectStorageClient.downloadSimpleObject(objectUrl);
//            String dashboard = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            log.info("Dashboard fetched from object storage: tenantId={}, dashboardId={}, size={} bytes",
//                    tenantId, dashboardId, dashboard.length());
//            return dashboard;
//        } catch (Exception e) {
//            log.error("Failed to fetch dashboard for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
//            throw e;
//        }
//    }
    public String getDashboard(String tenantId, String dashboardId) throws Exception {
        log.info("[getDashboard] Fetching for tenantId={}, dashboardId={}", tenantId, dashboardId);

        DashboardMetadata meta = metadataRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new RuntimeException("Dashboard metadata not found!"));

        String objectUrl = meta.getObjectUrl();
        if (objectUrl == null || objectUrl.isEmpty())
            throw new RuntimeException("Dashboard object URL missing in metadata");

        try {
            // Attempt normal fetch
            org.springframework.core.io.Resource resource =
                    objectStorageClient.downloadSimpleObject(objectUrl);
            String dashboard = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("Dashboard fetched from object storage: tenantId={}, dashboardId={}, size={} bytes",
                    tenantId, dashboardId, dashboard.length());
            return dashboard;

        } catch (Exception e) {
            String msg = e.getMessage();
            // Detect 'key does not exist'
            if (msg != null && msg.contains("The specified key does not exist")) {
                log.warn("Dashboard object missing in storage; will auto-create minimal dashboard for tenantId={}, dashboardId={}", tenantId, dashboardId);

                // --- Compose minimal dashboard JSON from metadata ---
                Map<String, Object> dashboard = new HashMap<>();
                dashboard.put("id", dashboardId);
                dashboard.put("title", meta.getTitle() != null ? meta.getTitle() : "Untitled");
                dashboard.put("widgets", new ArrayList<>());
                dashboard.put("layouts", Map.of("lg", List.of(), "md", List.of(), "sm", List.of(), "xs", List.of()));
                dashboard.put("settings", Map.of());
                dashboard.put("assignedCustomers", meta.getAssignedCustomers() != null ? meta.getAssignedCustomers() : List.of());
                dashboard.put("timewindow", Map.of("displayValue", "Last 15 minutes", "value", "15_minutes", "type", "REALTIME"));
                dashboard.put("version", meta.getVersion() != null ? meta.getVersion() : "1.0.0");
                dashboard.put("createdTime", meta.getCreatedTime() != null ? meta.getCreatedTime() : java.time.OffsetDateTime.now().toString());

                String minimalJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dashboard);

                // --- Use your saveDashboard() logic for upload ---
                saveDashboard(tenantId, dashboardId, minimalJson);

                log.info("Minimal dashboard JSON created and uploaded for tenantId={}, dashboardId={}", tenantId, dashboardId);

                return minimalJson;
            }
            // Otherwise propagate error
            log.error("Failed to fetch dashboard (object missing and cannot self-heal): tenantId={}, dashboardId={}, err={}",
                    tenantId, dashboardId, e.getMessage());
            throw e;
        }
    }


    public String getDashboardPresignedUrl(
            String tenantId, String dashboardId, int expiry) throws Exception {
        log.info("[getDashboardPresignedUrl] Requesting for tenantId={}, dashboardId={}, expiry={}",
                tenantId, dashboardId, expiry);
        try {
            DashboardMetadata meta = metadataRepository.findByIdAndTenantId(dashboardId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Dashboard metadata not found!"));
            // Use standard OBSS API - pass logical fields, not key!
            Map<String, String> map = objectStorageClient.getLatestPresigned(
                    tenantId,
                    dashboardId, // this is deviceId in OBSS
                    "dashboard", // objectType
                    expiry
            );
            String url = map.get("url");
            log.info("Presigned URL fetched: tenantId={}, dashboardId={}, url={}", tenantId, dashboardId, url);
            return url;
        } catch (Exception e) {
            log.error("Failed to fetch presigned URL for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
            throw e;
        }
    }

    public List<DashboardDto> listDashboardsForTenant(String tenantId) {
        List<DashboardDto> dashboardList = new ArrayList<>();
        try {
            List<DashboardMetadata> metas = metadataRepository.findByTenantId(tenantId);
            for (DashboardMetadata meta : metas) {
                DashboardDto dto = getDashboardDto(meta);
                dashboardList.add(dto);
            }
            log.info("Listed {} dashboards for tenantId={}", dashboardList.size(), tenantId);
        } catch (Exception e) {
            log.error("Failed to list dashboards for tenantId={}: {}", tenantId, e.getMessage(), e);
        }
        return dashboardList;
    }

    private static DashboardDto getDashboardDto(DashboardMetadata meta) {
        DashboardDto dto = new DashboardDto();
        dto.setId(meta.getId());
        dto.setTitle(meta.getTitle());
        dto.setCreatedTime(meta.getCreatedTime());
        dto.setAssignedCustomers(meta.getAssignedCustomers());
        dto.setIsPublic(meta.getIsPublic());
        dto.setVersion(meta.getVersion());
        // Return full object info (for auditing, tracing, download, preview)
        dto.setObjectUrl(meta.getObjectUrl());
        dto.setObjectSize(meta.getObjectSize());
        dto.setFileName(meta.getFileName());
        dto.setLastUpdatedTime(meta.getLastUpdatedTime());
        return dto;
    }
}

//package com.iotmining.services.dmss.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.iotmining.services.dmss.client.ObjectStorageClient;
//import com.iotmining.services.dmss.dto.DashboardDto;
//import com.iotmining.services.dmss.util.CustomMultipartFile;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DashboardService {
//    private final ObjectStorageClient objectStorageClient;
//
//    private static final String DASHBOARD_OBJECT_TYPE = "dashboard";
//    private static final String DASHBOARD_CATEGORY = "UI";
//
//    public Map<String, Object> saveDashboard(
//            String tenantId, String dashboardId, String dashboardJson) throws Exception {
//
//        String finalDashboardId = (dashboardId == null || dashboardId.isBlank())
//                ? UUID.randomUUID().toString()
//                : dashboardId;
//
//        log.info("[saveDashboard] Called with tenantId={}, dashboardId={}, payloadSize={} bytes",
//                tenantId, finalDashboardId, dashboardJson != null ? dashboardJson.length() : 0);
//
//        try {
//            MultipartFile file = new CustomMultipartFile(
//                    finalDashboardId + ".json",
//                    finalDashboardId + ".json",
//                    "application/json",
//                    dashboardJson.getBytes(StandardCharsets.UTF_8)
//            );
//            log.debug("Created CustomMultipartFile for dashboard '{}', filename='{}'", finalDashboardId, finalDashboardId + ".json");
//
//            // Use uploadSimple (calls /upload-simple endpoint in object storage service)
//            Map<String, Object> result = objectStorageClient.uploadSimple(
//                    file,
//                    tenantId,
//                    finalDashboardId,    // dashboardId used as objectId
//                    DASHBOARD_OBJECT_TYPE,
//                    DASHBOARD_CATEGORY,
//                    86400
//            );
//            Map<String, Object> response = new HashMap<>(result);
//            response.put("dashboardId", finalDashboardId);
//            log.info("Dashboard uploaded to object storage (no date/time in path) for tenantId={}, dashboardId={}", tenantId, finalDashboardId);
//            return response;
//        } catch (Exception e) {
//            log.error("Failed to save dashboard for tenantId={}, dashboardId={}: {}", tenantId, finalDashboardId, e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    public String getDashboard(
//            String tenantId, String dashboardId) throws Exception {
//        log.info("[getDashboard] Fetching for tenantId={}, dashboardId={}", tenantId, dashboardId);
//        try {
//            org.springframework.core.io.Resource resource =
//                    objectStorageClient.downloadLatestObject(tenantId, dashboardId, DASHBOARD_OBJECT_TYPE);
//            String dashboard = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            log.info("Dashboard fetched from object storage: tenantId={}, dashboardId={}, size={} bytes",
//                    tenantId, dashboardId, dashboard.length());
//            return dashboard;
//        } catch (Exception e) {
//            log.error("Failed to fetch dashboard for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    public String getDashboardPresignedUrl(
//            String tenantId, String dashboardId, int expiry) throws Exception {
//        log.info("[getDashboardPresignedUrl] Requesting for tenantId={}, dashboardId={}, expiry={}",
//                tenantId, dashboardId, expiry);
//        try {
//            Map<String, String> map = objectStorageClient.getLatestPresigned(tenantId, dashboardId, DASHBOARD_OBJECT_TYPE, expiry);
//            String url = map.get("url");
//            log.info("Presigned URL fetched: tenantId={}, dashboardId={}, url={}", tenantId, dashboardId, url);
//            return url;
//        } catch (Exception e) {
//            log.error("Failed to fetch presigned URL for tenantId={}, dashboardId={}: {}", tenantId, dashboardId, e.getMessage(), e);
//            throw e;
//        }
//    }
//
//    public List<DashboardDto> listDashboardsForTenant(String tenantId) {
//        List<DashboardDto> dashboardList = new ArrayList<>();
//        try {
//            // 1. List all dashboard JSON object keys for this tenant
//            List<String> objectKeys = objectStorageClient.listSimpleObjects(
//                    tenantId,
//                    DASHBOARD_OBJECT_TYPE,
//                    DASHBOARD_CATEGORY
//            );
//            ObjectMapper objectMapper = new ObjectMapper();
//            for (String key : objectKeys) {
//                try {
//                    // 2. Download and parse JSON content
//                    org.springframework.core.io.Resource resource = objectStorageClient.downloadSimpleObject(key);
//                    String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//                    DashboardDto dto = objectMapper.readValue(json, DashboardDto.class);
//                    dashboardList.add(dto);
//                } catch (Exception ex) {
//                    log.warn("Failed to parse dashboard at {}: {}", key, ex.getMessage());
//                }
//            }
//            log.info("Listed {} dashboards for tenantId={}", dashboardList.size(), tenantId);
//        } catch (Exception e) {
//            log.error("Failed to list dashboards for tenantId={}: {}", tenantId, e.getMessage(), e);
//        }
//        return dashboardList;
//    }
//}
