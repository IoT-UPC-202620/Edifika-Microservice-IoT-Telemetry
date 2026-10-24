package com.edifika.telemetry.domain.model.queries;

import com.edifika.telemetry.domain.model.valueobjects.Magnitude;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record GetReadingSeriesQuery(
        String deviceCode,
        Magnitude magnitude,
        Instant from,
        Instant to,
        String bucket
) {
    public GetReadingSeriesQuery {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("deviceCode is required");
        }
        if (magnitude == null) {
            throw new IllegalArgumentException("magnitude is required");
        }
        if (from == null) {
            from = Instant.now().minus(1, ChronoUnit.HOURS);
        }
        if (to == null) {
            to = Instant.now();
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "raw";
        } else {
            bucket = bucket.toLowerCase();
        }

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be strictly before to");
        }

        Duration duration = Duration.between(from, to);
        switch (bucket) {
            case "raw" -> {
                if (duration.compareTo(Duration.ofHours(24)) > 0) {
                    throw new IllegalArgumentException("raw range cannot exceed 24 hours");
                }
            }
            case "1m" -> {
                if (duration.compareTo(Duration.ofDays(7)) > 0) {
                    throw new IllegalArgumentException("1m range cannot exceed 7 days");
                }
            }
            case "1h" -> {
                if (duration.compareTo(Duration.ofDays(90)) > 0) {
                    throw new IllegalArgumentException("1h range cannot exceed 90 days");
                }
            }
            default -> throw new IllegalArgumentException("Invalid bucket: " + bucket + ". Allowed: raw, 1m, 1h");
        }
    }
}
