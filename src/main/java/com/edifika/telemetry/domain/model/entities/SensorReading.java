package com.edifika.telemetry.domain.model.entities;

import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sensor_readings")
@IdClass(SensorReadingId.class)
@Getter
@NoArgsConstructor
public class SensorReading {

    @Id
    @Column(name = "device_code", length = 64, nullable = false)
    private String deviceCode;

    @Id
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "magnitude", length = 32, nullable = false)
    private Magnitude magnitude;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "unit", length = 16, nullable = false)
    private String unit;

    @Column(name = "time_suspect", nullable = false)
    private boolean timeSuspect;

    @Column(name = "source_event_id", length = 64)
    private String sourceEventId;

    @Column(name = "building_id")
    private Long buildingId;

    public SensorReading(String deviceCode, Instant capturedAt, Magnitude magnitude,
                         Double value, String unit, boolean timeSuspect,
                         String sourceEventId, Long buildingId) {
        this.deviceCode = deviceCode;
        this.capturedAt = capturedAt;
        this.magnitude = magnitude;
        this.value = value;
        this.unit = unit;
        this.timeSuspect = timeSuspect;
        this.sourceEventId = sourceEventId;
        this.buildingId = buildingId;
    }
}
