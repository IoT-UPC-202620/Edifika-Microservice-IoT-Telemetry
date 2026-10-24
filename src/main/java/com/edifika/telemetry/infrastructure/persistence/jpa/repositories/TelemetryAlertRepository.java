package com.edifika.telemetry.infrastructure.persistence.jpa.repositories;

import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelemetryAlertRepository extends JpaRepository<TelemetryAlert, Long> {

    List<TelemetryAlert> findByDeviceCodeOrderByOccurredAtDesc(String deviceCode);

    List<TelemetryAlert> findByDeviceCodeAndStateOrderByOccurredAtDesc(String deviceCode, String state);

    List<TelemetryAlert> findByStateOrderByOccurredAtDesc(String state);

    List<TelemetryAlert> findAllByOrderByOccurredAtDesc();

    List<TelemetryAlert> findByOccurredAtBetweenOrderByOccurredAtDesc(Instant from, Instant to);
}
