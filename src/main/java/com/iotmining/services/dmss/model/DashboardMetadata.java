package com.iotmining.services.dmss.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "dashboard_metadata")
@Data
public class DashboardMetadata {
    @Id
    private String id; // dashboardId

    @Column(nullable = false)
    private String tenantId;

    private String title;
    private String createdTime;
    @ElementCollection
    private List<String> assignedCustomers;
    private Boolean isPublic;
    private String version;

    // Add these fields:
    private String objectUrl;         // Minio/object store path
    private Long objectSize;          // bytes
    private String fileName;          // filename in Minio (optional)
    private String lastUpdatedTime;   // ISO8601 string
}
