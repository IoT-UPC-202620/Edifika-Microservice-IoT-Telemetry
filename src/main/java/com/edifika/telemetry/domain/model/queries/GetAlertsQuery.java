package com.edifika.telemetry.domain.model.queries;

import java.time.Instant;

public record GetAlertsQuery(
        String deviceCode,
        String state,
        Instant from,
        Instant to
) {
}
