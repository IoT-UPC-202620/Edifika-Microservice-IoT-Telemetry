package com.edifika.telemetry.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "telemetry_alerts")
@Getter
@NoArgsConstructor
public class TelemetryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_code", nullable = false, length = 64)
    private String deviceCode;

    @Column(name = "kind", nullable = false, length = 64)
    private String kind;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public TelemetryAlert(String deviceCode, String kind, String state, String details, Instant occurredAt) {
        this.deviceCode = deviceCode;
        this.kind = kind;
        this.state = state;
        this.details = details;
        this.occurredAt = occurredAt;
    }
}
