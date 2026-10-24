package com.edifika.telemetry.domain.services;

import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;

public interface TelemetryIngestionService {
    void ingestReading(IntegrationEnvelope env);
    void applyAlert(IntegrationEnvelope env);
    void applyDeviceStatus(IntegrationEnvelope env);
    void applyRegistry(IntegrationEnvelope env);
}
