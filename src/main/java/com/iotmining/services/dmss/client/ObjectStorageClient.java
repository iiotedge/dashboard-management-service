package com.iotmining.services.dmss.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "object-storage-service", url = "${services.object-storage-service.url:http://localhost:8060}")
public interface ObjectStorageClient {

    @PostMapping(value = "/api/v1/object/upload-simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> uploadSimple(
            @RequestPart("file") MultipartFile file,
            @RequestPart("tenantId") String tenantId,
            @RequestPart("deviceId") String dashboardId,
            @RequestPart("objectType") String objectType,
            @RequestPart("category") String category,
            @RequestParam("expiry") int expiry
    );

    @GetMapping("/api/v1/object/download/latest")
    @ResponseBody
    org.springframework.core.io.Resource downloadLatestObject(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("deviceId") String deviceId,
            @RequestParam("objectType") String objectType
    );

    @GetMapping("/api/v1/object/latest/presigned")
    Map<String, String> getLatestPresigned(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("deviceId") String deviceId,
            @RequestParam("objectType") String objectType,
            @RequestParam(value = "expiry", defaultValue = "300") int expiry
    );

    @GetMapping("/api/v1/object/download-simple")
    @ResponseBody
    org.springframework.core.io.Resource downloadSimpleObject(
            @RequestParam("key") String key
    );

    @GetMapping("/api/v1/object/simple/presigned")
    Map<String, String> getPresignedSimple(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("deviceId") String deviceId,
            @RequestParam("objectType") String objectType,
            @RequestParam(value = "category", defaultValue = "UI") String category,
            @RequestParam(value = "expiry", defaultValue = "300") int expiry
    );
}
