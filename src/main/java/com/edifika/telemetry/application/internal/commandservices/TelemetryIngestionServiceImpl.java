package com.edifika.telemetry.application.internal.commandservices;

import com.edifika.telemetry.application.internal.eventhandlers.TelemetryIntegrationEventHandler;
import com.edifika.telemetry.application.internal.messaging.ProcessedEventGuard;
import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import com.edifika.telemetry.domain.model.valueobjects.ConnectionStatus;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.domain.services.TelemetryIngestionService;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.DeviceProjectionRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.TelemetryAlertRepository;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class TelemetryIngestionServiceImpl implements TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SensorReadingRepository sensorReadingRepository;
    private final DeviceProjectionRepository deviceProjectionRepository;
    private final TelemetryAlertRepository telemetryAlertRepository;
    private final ProcessedEventGuard processedEventGuard;
    private final TelemetryIntegrationEventHandler eventHandler;

    public TelemetryIngestionServiceImpl(
            SensorReadingRepository sensorReadingRepository,
            DeviceProjectionRepository deviceProjectionRepository,
            TelemetryAlertRepository telemetryAlertRepository,
            ProcessedEventGuard processedEventGuard,
            TelemetryIntegrationEventHandler eventHandler
    ) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.deviceProjectionRepository = deviceProjectionRepository;
        this.telemetryAlertRepository = telemetryAlertRepository;
        this.processedEventGuard = processedEventGuard;
        this.eventHandler = eventHandler;
    }

    private void validateEnvelope(IntegrationEnvelope env) {
        if (env.eventVersion() != 1) {
            log.warn("Rejected envelope {} due to unsupported eventVersion: {}", env.eventId(), env.eventVersion());
            throw new AmqpRejectAndDontRequeueException("Unsupported eventVersion: " + env.eventVersion());
        }
    }

    @Override
    @Transactional
    public void ingestReading(IntegrationEnvelope env) {
        validateEnvelope(env);
        if (!processedEventGuard.firstTime(env.eventId())) {
            log.debug("Ignoring duplicate reading eventId {}", env.eventId());
            return;
        }

        Map<String, Object> payload = env.payload();
        if (payload == null) {
            return;
        }

        String deviceId = (String) payload.get("deviceId");
        if (deviceId == null) {
            deviceId = (String) payload.get("deviceCode");
        }
        if (deviceId == null) {
            log.warn("Missing deviceId in reading envelope {}", env.eventId());
            return;
        }

        String sensor = (String) payload.get("sensor");
        Magnitude magnitude = parseMagnitude(sensor);
        if (magnitude == null) {
            log.debug("Ignoring reading with unknown sensor: {}", sensor);
            return;
        }

        Number valNum = (Number) payload.get("value");
        double value = valNum != null ? valNum.doubleValue() : 0.0;

        String unit = normalizeUnit(magnitude, (String) payload.get("unit"));
        Instant capturedAt = parseInstant(payload.get("capturedAt"), env.occurredAt());
        boolean timeSuspect = Boolean.TRUE.equals(payload.get("timeSuspect"));

        DeviceProjection projection = deviceProjectionRepository.findByDeviceCode(deviceId).orElse(null);
        Long buildingId;
        if (projection == null) {
            DeviceProjection minimal = new DeviceProjection(deviceId, env.gatewayCode());
            projection = deviceProjectionRepository.save(minimal);
            buildingId = env.buildingId() != null ? env.buildingId() : projection.getBuildingId();
        } else {
            buildingId = projection.getBuildingId();
        }

        SensorReading reading = new SensorReading(
                deviceId,
                capturedAt,
                magnitude,
                value,
                unit,
                timeSuspect,
                env.eventId(),
                buildingId
        );

        sensorReadingRepository.insertIgnoringDuplicates(reading);
    }

    @Override
    @Transactional
    public void applyAlert(IntegrationEnvelope env) {
        validateEnvelope(env);
        if (!processedEventGuard.firstTime(env.eventId())) {
            log.debug("Ignoring duplicate alert eventId {}", env.eventId());
            return;
        }

        Map<String, Object> payload = env.payload();
        if (payload == null) {
            return;
        }

        String deviceId = (String) payload.get("deviceId");
        if (deviceId == null) {
            deviceId = (String) payload.get("deviceCode");
        }
        String alert = (String) payload.get("alert");
        String state = (String) payload.get("state");
        String stateUpper = state != null ? state.toUpperCase() : "RAISED";

        String detailsJson;
        try {
            detailsJson = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            detailsJson = "{}";
        }

        TelemetryAlert alertEntity = new TelemetryAlert(deviceId, alert, stateUpper, detailsJson, env.occurredAt());
        telemetryAlertRepository.save(alertEntity);

        DeviceProjection projection = deviceProjectionRepository.findByDeviceCode(deviceId).orElse(null);
        Long buildingId = projection != null ? projection.getBuildingId() : env.buildingId();
        String gatewayCode = projection != null ? projection.getGatewayCode() : env.gatewayCode();

        if ("RAISED".equals(stateUpper)) {
            if (alert != null && alert.startsWith("sensor_fault_")) {
                String sensor = (String) payload.get("sensor");
                if (sensor == null) {
                    sensor = alert.substring("sensor_fault_".length());
                }
                eventHandler.publishSensorFaulty(deviceId, sensor, buildingId, gatewayCode);
            } else if ("device_offline".equalsIgnoreCase(alert)) {
                if (projection != null) {
                    projection.markOffline();
                    deviceProjectionRepository.save(projection);
                }
                eventHandler.publishDeviceWentOffline(deviceId, env.occurredAt(), buildingId, gatewayCode);
            }
        }
    }

    @Override
    @Transactional
    public void applyDeviceStatus(IntegrationEnvelope env) {
        validateEnvelope(env);
        if (!processedEventGuard.firstTime(env.eventId())) {
            log.debug("Ignoring duplicate status eventId {}", env.eventId());
            return;
        }

        Map<String, Object> payload = env.payload();
        if (payload == null) {
            return;
        }

        String deviceId = (String) payload.get("deviceId");
        if (deviceId == null) {
            deviceId = (String) payload.get("deviceCode");
        }
        String event = (String) payload.get("event");

        DeviceProjection projection = deviceProjectionRepository.findByDeviceCode(deviceId).orElse(null);
        if (projection == null) {
            projection = new DeviceProjection(deviceId, env.gatewayCode());
            projection = deviceProjectionRepository.save(projection);
        }

        if (projection.getLastHeartbeatAt() != null && env.occurredAt().isBefore(projection.getLastHeartbeatAt())) {
            log.debug("Ignoring stale status for device {}, occurredAt={}", deviceId, env.occurredAt());
            return;
        }

        if ("online".equalsIgnoreCase(event)) {
            projection.markOnline(env.occurredAt());
            deviceProjectionRepository.save(projection);
        } else if ("offline".equalsIgnoreCase(event)) {
            if (projection.getConnectionStatus() != ConnectionStatus.OFFLINE) {
                projection.markOffline();
                deviceProjectionRepository.save(projection);
                Instant lastSeenAt = parseInstant(payload.get("lastSeen"), env.occurredAt());
                eventHandler.publishDeviceWentOffline(deviceId, lastSeenAt, projection.getBuildingId(), projection.getGatewayCode());
            }
        }
    }

    @Override
    @Transactional
    public void applyRegistry(IntegrationEnvelope env) {
        validateEnvelope(env);
        if (!processedEventGuard.firstTime(env.eventId())) {
            log.debug("Ignoring duplicate registry eventId {}", env.eventId());
            return;
        }

        Map<String, Object> payload = env.payload();
        if (payload == null) {
            return;
        }

        String eventType = env.eventType();
        String deviceCode = (String) payload.get("deviceCode");
        if (deviceCode == null) {
            deviceCode = (String) payload.get("deviceId");
        }

        if ("DeviceDecommissioned".equalsIgnoreCase(eventType) || "iot.device-decommissioned".equalsIgnoreCase(eventType)) {
            deviceProjectionRepository.findByDeviceCode(deviceCode).ifPresent(p -> {
                p.decommission();
                deviceProjectionRepository.save(p);
            });
            return;
        }

        // DeviceRegistered or DeviceUpdated
        Number devIdNum = (Number) payload.get("deviceId");
        Long deviceId = devIdNum != null ? devIdNum.longValue() : null;
        String gatewayCode = (String) payload.get("gatewayCode");
        Number bIdNum = (Number) payload.get("buildingId");
        Long buildingId = bIdNum != null ? bIdNum.longValue() : null;
        String name = (String) payload.get("name");
        String status = (String) payload.get("status");
        ConnectionStatus connStatus = "OFFLINE".equalsIgnoreCase(status) ? ConnectionStatus.OFFLINE : ConnectionStatus.ACTIVE;

        DeviceProjection projection = deviceProjectionRepository.findByDeviceCode(deviceCode).orElse(null);
        if (projection == null) {
            projection = new DeviceProjection(deviceCode, deviceId, gatewayCode, buildingId, name, connStatus);
        } else {
            projection.updateRegistry(deviceId, gatewayCode, buildingId, name, connStatus);
        }
        deviceProjectionRepository.save(projection);
    }

    private Magnitude parseMagnitude(String sensor) {
        if (sensor == null) return null;
        try {
            return Magnitude.valueOf(sensor.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeUnit(Magnitude magnitude, String unit) {
        if (magnitude == Magnitude.TEMPERATURE) {
            return "°C";
        }
        if (magnitude == Magnitude.HUMIDITY) {
            return "%";
        }
        return unit != null ? unit : "";
    }

    private Instant parseInstant(Object val, Instant fallback) {
        if (val == null) return fallback;
        if (val instanceof Instant i) return i;
        if (val instanceof String s) {
            try {
                return Instant.parse(s);
            } catch (Exception ignored) {
                return fallback;
            }
        }
        if (val instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        return fallback;
    }
}
