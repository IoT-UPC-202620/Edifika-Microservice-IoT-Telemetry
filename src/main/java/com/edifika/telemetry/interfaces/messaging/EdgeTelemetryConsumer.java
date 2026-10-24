package com.edifika.telemetry.interfaces.messaging;

import com.edifika.telemetry.domain.services.TelemetryIngestionService;
import com.edifika.telemetry.infrastructure.messaging.rabbitmq.configuration.RabbitMqConfiguration;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EdgeTelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(EdgeTelemetryConsumer.class);

    private final TelemetryIngestionService ingestionService;

    public EdgeTelemetryConsumer(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @RabbitListener(queues = RabbitMqConfiguration.EDGE_EVENTS_QUEUE)
    public void onMessage(IntegrationEnvelope envelope) {
        if (envelope == null) return;
        String eventType = envelope.eventType();
        if ("Reading".equalsIgnoreCase(eventType)) {
            ingestionService.ingestReading(envelope);
        } else if ("Alert".equalsIgnoreCase(eventType)) {
            ingestionService.applyAlert(envelope);
        } else if ("DeviceStatus".equalsIgnoreCase(eventType)) {
            ingestionService.applyDeviceStatus(envelope);
        } else {
            log.warn("Unknown edge eventType: {}", eventType);
        }
    }
}
