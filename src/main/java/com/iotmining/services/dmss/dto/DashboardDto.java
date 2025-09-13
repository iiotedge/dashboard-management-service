package com.iotmining.services.dmss.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardDto {
    private String id;
    private String title;
    private String createdTime;
    private List<String> assignedCustomers;
    private Boolean isPublic;
    private Object timewindow;
    private String version;

    // Add these (for list and detail APIs)
    private String objectUrl;
    private Long objectSize;
    private String fileName;
    private String lastUpdatedTime;
}
