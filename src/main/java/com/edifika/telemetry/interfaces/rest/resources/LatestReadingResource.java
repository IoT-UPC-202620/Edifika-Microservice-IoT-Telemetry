package com.edifika.telemetry.interfaces.rest.resources;

import java.time.Instant;

public record LatestReadingResource(
        String magnitude,
        Double value,
        String unit,
        Instant capturedAt
) {
}
