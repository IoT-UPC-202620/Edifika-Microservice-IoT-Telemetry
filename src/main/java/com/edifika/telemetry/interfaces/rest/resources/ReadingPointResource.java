package com.edifika.telemetry.interfaces.rest.resources;

import java.time.Instant;

public record ReadingPointResource(
        Instant t,
        Double value,
        Double min,
        Double max,
        Long count
) {
}
