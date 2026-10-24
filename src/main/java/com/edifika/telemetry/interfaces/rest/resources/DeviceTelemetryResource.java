package com.edifika.telemetry.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

public record DeviceTelemetryResource(
        String deviceCode,
        Long deviceId,
        String name,
        Long buildingId,
        String connectionStatus,
        Instant lastHeartbeatAt,
        List<LatestReadingResource> latest
) {
}
