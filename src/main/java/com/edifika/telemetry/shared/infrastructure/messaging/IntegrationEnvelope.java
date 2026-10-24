package com.edifika.telemetry.shared.infrastructure.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IntegrationEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        Long buildingId,
        String gatewayCode,
        String correlationId,
        Map<String, Object> payload
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static IntegrationEnvelope of(String eventType, Long buildingId, String gatewayCode, String correlationId, Object payload) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> payloadMap;
        if (payload instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) m;
            payloadMap = casted;
        } else if (payload != null) {
            payloadMap = MAPPER.convertValue(payload, new TypeReference<>() {});
        } else {
            payloadMap = Map.of();
        }
        return new IntegrationEnvelope(
                eventId,
                eventType,
                1,
                Instant.now(),
                "iot-telemetry",
                buildingId,
                gatewayCode,
                correlationId,
                payloadMap
        );
    }
}
