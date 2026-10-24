package com.edifika.telemetry.interfaces.messaging;

import com.edifika.telemetry.domain.services.TelemetryIngestionService;
import com.edifika.telemetry.infrastructure.messaging.rabbitmq.configuration.RabbitMqConfiguration;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeviceRegistryConsumer {

    private final TelemetryIngestionService ingestionService;

    public DeviceRegistryConsumer(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @RabbitListener(queues = RabbitMqConfiguration.DEVICE_REGISTRY_QUEUE)
    public void onMessage(IntegrationEnvelope envelope) {
        if (envelope == null) return;
        ingestionService.applyRegistry(envelope);
    }
}
