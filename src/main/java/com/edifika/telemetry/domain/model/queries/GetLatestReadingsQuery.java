package com.edifika.telemetry.domain.model.queries;

public record GetLatestReadingsQuery(String deviceCode) {
    public GetLatestReadingsQuery {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("deviceCode is required");
        }
    }
}
