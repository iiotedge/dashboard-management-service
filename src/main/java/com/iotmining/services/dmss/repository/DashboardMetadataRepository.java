package com.iotmining.services.dmss.repository;

import com.iotmining.services.dmss.model.DashboardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DashboardMetadataRepository extends JpaRepository<DashboardMetadata, String> {
    List<DashboardMetadata> findByTenantId(String tenantId);
    Optional<DashboardMetadata> findByIdAndTenantId(String id, String tenantId);
}
