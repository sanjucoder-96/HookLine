package com.hookline.repo;

import com.hookline.domain.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    List<Endpoint> findByActiveTrue();

    /** A tenant's active endpoints - the fan-out targets for that tenant's events. */
    List<Endpoint> findByTenantIdAndActiveTrue(UUID tenantId);
}
