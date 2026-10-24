package com.edifika.telemetry.infrastructure.persistence.jpa.repositories;

import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.entities.SensorReadingId;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, SensorReadingId> {

    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO sensor_readings (device_code, captured_at, magnitude, value, unit, time_suspect, source_event_id, building_id)
        VALUES (:deviceCode, :capturedAt, :magnitude, :value, :unit, :timeSuspect, :sourceEventId, :buildingId)
        ON CONFLICT (device_code, captured_at, magnitude) DO NOTHING
        """, nativeQuery = true)
    int insertIgnoringDuplicatesNative(
            @Param("deviceCode") String deviceCode,
            @Param("capturedAt") Instant capturedAt,
            @Param("magnitude") String magnitude,
            @Param("value") Double value,
            @Param("unit") String unit,
            @Param("timeSuspect") boolean timeSuspect,
            @Param("sourceEventId") String sourceEventId,
            @Param("buildingId") Long buildingId
    );

    @Transactional
    default int insertIgnoringDuplicates(SensorReading r) {
        return insertIgnoringDuplicatesNative(
                r.getDeviceCode(),
                r.getCapturedAt(),
                r.getMagnitude().name(),
                r.getValue(),
                r.getUnit(),
                r.isTimeSuspect(),
                r.getSourceEventId(),
                r.getBuildingId()
        );
    }

    List<SensorReading> findByDeviceCodeAndMagnitudeAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
            String deviceCode, Magnitude magnitude, Instant from, Instant to
    );

    Optional<SensorReading> findFirstByDeviceCodeAndMagnitudeOrderByCapturedAtDesc(
            String deviceCode, Magnitude magnitude
    );

    Optional<SensorReading> findFirstByDeviceCodeAndMagnitudeAndTimeSuspectFalseOrderByCapturedAtDesc(
            String deviceCode, Magnitude magnitude
    );

    interface BucketRow {
        Instant getT();
        Double getValue();
        Double getMin();
        Double getMax();
        Long getCount();
    }

    @Query(value = """
        SELECT date_trunc(:unit, captured_at) AS t,
               avg(value) AS value,
               min(value) AS min,
               max(value) AS max,
               count(*) AS count
        FROM sensor_readings
        WHERE device_code = :code
          AND magnitude = :mag
          AND captured_at >= :from
          AND captured_at < :to
        GROUP BY 1
        ORDER BY 1 DESC
        LIMIT 5000
        """, nativeQuery = true)
    List<BucketRow> aggregateReadings(
            @Param("code") String deviceCode,
            @Param("mag") String magnitude,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("unit") String unit
    );
}
