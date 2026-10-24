package com.edifika.telemetry.interfaces.messaging;

import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import com.edifika.telemetry.domain.model.valueobjects.ConnectionStatus;
import com.edifika.telemetry.infrastructure.messaging.rabbitmq.configuration.RabbitMqConfiguration;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.DeviceProjectionRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import com.edifika.telemetry.support.ContractAssertions;
import com.edifika.telemetry.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers
class EdgeTelemetryConsumerIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private DeviceProjectionRepository deviceProjectionRepository;

    private static final String TEST_OUTBOUND_QUEUE = "test.telemetry-outbound";

    @BeforeEach
    void setUpQueue() {
        Queue testQueue = QueueBuilder.durable(TEST_OUTBOUND_QUEUE).build();
        amqpAdmin.declareQueue(testQueue);
        Binding binding = BindingBuilder.bind(testQueue)
                .to(new TopicExchange(RabbitMqConfiguration.EVENTS_EXCHANGE))
                .with("telemetry.#");
        amqpAdmin.declareBinding(binding);
        amqpAdmin.purgeQueue(TEST_OUTBOUND_QUEUE);
        amqpAdmin.purgeQueue(RabbitMqConfiguration.EDGE_EVENTS_DLQ);
    }

    @Test
    void hundredReadingsStoredWithP95Under500ms() {
        deviceProjectionRepository.save(new DeviceProjection(
                "esp32-perf-01", 100L, "gw-perf-01", 12L, "Perf Device", ConnectionStatus.ACTIVE
        ));

        Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(3600);
        List<IntegrationEnvelope> envelopes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Instant captured = baseTime.plusSeconds(i);
            IntegrationEnvelope env = new IntegrationEnvelope(
                    UUID.randomUUID().toString().replace("-", ""),
                    "Reading",
                    1,
                    captured,
                    "edge-gateway",
                    null,
                    "gw-perf-01",
                    null,
                    Map.of(
                            "deviceId", "esp32-perf-01",
                            "sensor", "temperature",
                            "value", 20.0 + (i * 0.1),
                            "unit", "°C",
                            "capturedAt", captured.toString(),
                            "timeSuspect", false
                    )
            );
            envelopes.add(env);
        }

        long startNanos = System.nanoTime();
        for (IntegrationEnvelope env : envelopes) {
            rabbitTemplate.convertAndSend(RabbitMqConfiguration.EVENTS_EXCHANGE, "edge.reading", env);
        }

        // Await until all 100 readings are stored in the database
        await().atMost(Duration.ofSeconds(15)).until(() ->
                sensorReadingRepository.findByDeviceCodeAndMagnitudeAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
                        "esp32-perf-01",
                        com.edifika.telemetry.domain.model.valueobjects.Magnitude.TEMPERATURE,
                        baseTime.minusSeconds(1),
                        baseTime.plusSeconds(105)
                ).size() == 100
        );

        long totalElapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        double avgPerReadingMs = (double) totalElapsedMs / 100.0;
        System.out.printf("Ingested 100 readings in %d ms (avg %.2f ms/reading)%n", totalElapsedMs, avgPerReadingMs);
        assertTrue(avgPerReadingMs < 500, "Expected p95 / avg ingestion latency under 500ms, got: " + avgPerReadingMs);

        // Republish all 100 envelopes - verify idempotence
        for (IntegrationEnvelope env : envelopes) {
            rabbitTemplate.convertAndSend(RabbitMqConfiguration.EVENTS_EXCHANGE, "edge.reading", env);
        }

        // Wait a bit to ensure consumer processed republishes
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        int countAfterRepublish = sensorReadingRepository.findByDeviceCodeAndMagnitudeAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
                "esp32-perf-01",
                com.edifika.telemetry.domain.model.valueobjects.Magnitude.TEMPERATURE,
                baseTime.minusSeconds(1),
                baseTime.plusSeconds(105)
        ).size();

        assertEquals(100, countAfterRepublish, "Duplicate readings must be ignored; count should remain 100");
    }

    @Test
    void publishedEventsMatchContract() {
        deviceProjectionRepository.save(new DeviceProjection(
                "esp32-contract-01", 101L, "gw-contract-01", 12L, "Contract Device", ConnectionStatus.ACTIVE
        ));

        // 1. Send an alert that causes SensorFaulty
        IntegrationEnvelope alertEnv = new IntegrationEnvelope(
                UUID.randomUUID().toString().replace("-", ""),
                "Alert",
                1,
                Instant.now(),
                "edge-gateway",
                null,
                "gw-contract-01",
                null,
                Map.of(
                        "deviceId", "esp32-contract-01",
                        "alert", "sensor_fault_temperature",
                        "state", "raised",
                        "sensor", "temperature"
                )
        );
        rabbitTemplate.convertAndSend(RabbitMqConfiguration.EVENTS_EXCHANGE, "edge.alert", alertEnv);

        // 2. Send status offline that causes DeviceWentOffline
        IntegrationEnvelope offlineEnv = new IntegrationEnvelope(
                UUID.randomUUID().toString().replace("-", ""),
                "DeviceStatus",
                1,
                Instant.now(),
                "edge-gateway",
                null,
                "gw-contract-01",
                null,
                Map.of(
                        "deviceId", "esp32-contract-01",
                        "event", "offline",
                        "lastSeen", Instant.now().toString()
                )
        );
        rabbitTemplate.convertAndSend(RabbitMqConfiguration.EVENTS_EXCHANGE, "edge.device-status", offlineEnv);

        // Await published messages in test queue
        List<IntegrationEnvelope> received = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            Object msg = rabbitTemplate.receiveAndConvert(TEST_OUTBOUND_QUEUE, 1000);
            if (msg instanceof IntegrationEnvelope env) {
                received.add(env);
            }
            return received.size() >= 2;
        });

        IntegrationEnvelope sensorFaulty = received.stream()
                .filter(e -> "SensorFaulty".equals(e.eventType()))
                .findFirst().orElseThrow();
        IntegrationEnvelope deviceWentOffline = received.stream()
                .filter(e -> "DeviceWentOffline".equals(e.eventType()))
                .findFirst().orElseThrow();

        // Validate envelopes against envelope schema
        ContractAssertions.assertMatches("contracts/schemas/envelope.schema.json", sensorFaulty);
        ContractAssertions.assertMatches("contracts/schemas/envelope.schema.json", deviceWentOffline);

        // Validate payloads against payload schemas
        ContractAssertions.assertMatches("contracts/schemas/payloads/SensorFaulty.schema.json", sensorFaulty.payload());
        ContractAssertions.assertMatches("contracts/schemas/payloads/DeviceWentOffline.schema.json", deviceWentOffline.payload());
    }

    @Test
    void malformedGoesToDlq() {
        // Send raw unparseable bytes to edge events queue
        Message badMessage = MessageBuilder.withBody("INVALID_JSON_PAYLOAD".getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.send(RabbitMqConfiguration.EVENTS_EXCHANGE, "edge.reading", badMessage);

        // Verify that the message arrives in the DLQ
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            Message dlqMessage = rabbitTemplate.receive(RabbitMqConfiguration.EDGE_EVENTS_DLQ, 1000);
            return dlqMessage != null;
        });
    }
}
