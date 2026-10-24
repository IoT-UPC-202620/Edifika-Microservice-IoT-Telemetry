package com.edifika.telemetry.domain.model.entities;

import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SensorReadingId implements Serializable {
    private String deviceCode;
    private Instant capturedAt;
    private Magnitude magnitude;
}
