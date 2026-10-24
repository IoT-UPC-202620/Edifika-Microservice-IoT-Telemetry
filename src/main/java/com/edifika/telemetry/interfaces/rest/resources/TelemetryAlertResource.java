package com.edifika.telemetry.interfaces.rest.resources;

import java.time.Instant;
import java.util.Map;

public record TelemetryAlertResource(
        Long id,
        String deviceCode,
        String kind,
        String state,
        Map<String, Object> details,
        Instant occurredAt
) {
}
