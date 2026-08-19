package com.iotmining.services.dmss.service;

import com.iotmining.services.dmss.client.ObjectStorageClient;
import com.iotmining.services.dmss.dto.DashboardDto;
import com.iotmining.services.dmss.model.DashboardMetadata;
import com.iotmining.services.dmss.repository.DashboardMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock private ObjectStorageClient objectStorageClient;
    @Mock private DashboardMetadataRepository metadataRepository;

    private DashboardService dashboardService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        dashboardService = new DashboardService(objectStorageClient, metadataRepository);
    }

    @Test
    @DisplayName("createDashboard generates a fresh id and saves it when none is supplied")
    void createsWithGeneratedId() {
        DashboardDto dto = new DashboardDto();
        dto.setTitle("My Dashboard");
        when(metadataRepository.existsById(anyString())).thenReturn(false);

        Map<String, Object> result = dashboardService.createDashboard("tenant-1", dto);

        assertThat(result.get("dashboardId")).isNotNull();
        ArgumentCaptor<DashboardMetadata> captor = ArgumentCaptor.forClass(DashboardMetadata.class);
        verify(metadataRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("createDashboard rejects a client-supplied id that already exists, instead of silently overwriting it")
    void rejectsCollidingId() {
        // Regression test: DashboardMetadata.id has no @GeneratedValue, so
        // JpaRepository.save() on an existing id performs a merge/update, not a
        // rejected insert. Without the existsById guard, a caller supplying the
        // id of a dashboard belonging to a DIFFERENT tenant would silently
        // overwrite that row - including its tenantId - hijacking it into their
        // own tenant instead of failing.
        DashboardDto dto = new DashboardDto();
        dto.setId("existing-dashboard-id");
        dto.setTitle("Hijack attempt");
        when(metadataRepository.existsById("existing-dashboard-id")).thenReturn(true);

        assertThatThrownBy(() -> dashboardService.createDashboard("attacker-tenant", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing-dashboard-id");

        verify(metadataRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDashboard accepts a client-supplied id when it doesn't already exist")
    void acceptsFreshClientSuppliedId() {
        DashboardDto dto = new DashboardDto();
        dto.setId("brand-new-id");
        dto.setTitle("New");
        when(metadataRepository.existsById("brand-new-id")).thenReturn(false);

        Map<String, Object> result = dashboardService.createDashboard("tenant-1", dto);

        assertThat(result.get("dashboardId")).isEqualTo("brand-new-id");
        verify(metadataRepository).save(any());
    }
}
