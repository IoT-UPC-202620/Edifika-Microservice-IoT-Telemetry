package com.edifika.telemetry.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

public record ReadingSeriesResource(
        String deviceCode,
        String magnitude,
        String unit,
        String bucket,
        Instant from,
        Instant to,
        List<ReadingPointResource> points
) {
}
