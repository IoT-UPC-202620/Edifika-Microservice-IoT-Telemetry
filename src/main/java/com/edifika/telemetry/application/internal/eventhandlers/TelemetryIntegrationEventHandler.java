package com.edifika.telemetry.application.internal.eventhandlers;

import com.edifika.telemetry.infrastructure.messaging.rabbitmq.configuration.RabbitMqConfiguration;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TelemetryIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIntegrationEventHandler.class);

    private final RabbitTemplate rabbitTemplate;

    public TelemetryIntegrationEventHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDeviceWentOffline(String deviceCode, Instant lastSeenAt, Long buildingId, String gatewayCode) {
        String lastSeenStr = DateTimeFormatter.ISO_INSTANT.format(lastSeenAt != null ? lastSeenAt : Instant.now());
        Map<String, Object> payload = Map.of(
                "deviceCode", deviceCode,
                "lastSeenAt", lastSeenStr
        );
        IntegrationEnvelope envelope = IntegrationEnvelope.of(
                "DeviceWentOffline",
                buildingId,
                gatewayCode,
                null,
                payload
        );

        publishEnvelope("telemetry.device-went-offline", envelope);
        log.info("Published DeviceWentOffline for deviceCode={}, lastSeenAt={}", deviceCode, lastSeenStr);
    }

    public void publishSensorFaulty(String deviceCode, String sensor, Long buildingId, String gatewayCode) {
        String sensorLower = sensor != null ? sensor.toLowerCase() : "unknown";
        Map<String, Object> payload = Map.of(
                "deviceCode", deviceCode,
                "sensor", sensorLower
        );
        IntegrationEnvelope envelope = IntegrationEnvelope.of(
                "SensorFaulty",
                buildingId,
                gatewayCode,
                null,
                payload
        );

        publishEnvelope("telemetry.sensor-faulty", envelope);
        log.info("Published SensorFaulty for deviceCode={}, sensor={}", deviceCode, sensorLower);
    }

    private void publishEnvelope(String routingKey, IntegrationEnvelope envelope) {
        rabbitTemplate.convertAndSend(RabbitMqConfiguration.EVENTS_EXCHANGE, routingKey, envelope, message -> {
            MessageProperties props = message.getMessageProperties();
            props.setMessageId(envelope.eventId());
            props.setType(envelope.eventType());
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding("utf-8");
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setHeader("x-producer", envelope.producer());
            return message;
        });
    }
}
