package com.edifika.telemetry.infrastructure.persistence.jpa.repositories;

import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Cierra las alertas RAISED de un dispositivo y tipo ocurridas hasta {@code at}.
     * Los eventos se guardan como bitacora (una fila por raised/cleared); sin esto la
     * fila RAISED original quedaba abierta para siempre.
     */
    @Modifying
    @Query("update TelemetryAlert a set a.state = 'CLEARED' "
            + "where a.deviceCode = :deviceCode and a.kind = :kind and a.state = 'RAISED' and a.occurredAt <= :at")
    int resolveOpen(@Param("deviceCode") String deviceCode, @Param("kind") String kind, @Param("at") Instant at);
}
