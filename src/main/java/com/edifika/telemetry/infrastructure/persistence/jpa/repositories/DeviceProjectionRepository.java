package com.edifika.telemetry.infrastructure.persistence.jpa.repositories;

import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceProjectionRepository extends JpaRepository<DeviceProjection, Long> {

    Optional<DeviceProjection> findByDeviceCode(String deviceCode);

    List<DeviceProjection> findByBuildingId(Long buildingId);
}
