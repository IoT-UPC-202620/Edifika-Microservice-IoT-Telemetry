package com.edifika.telemetry.application.internal.commandservices;

import com.edifika.telemetry.application.internal.eventhandlers.TelemetryIntegrationEventHandler;
import com.edifika.telemetry.application.internal.messaging.ProcessedEventGuard;
import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import com.edifika.telemetry.domain.model.valueobjects.ConnectionStatus;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.DeviceProjectionRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.TelemetryAlertRepository;
import com.edifika.telemetry.shared.infrastructure.messaging.IntegrationEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceImplTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private DeviceProjectionRepository deviceProjectionRepository;

    @Mock
    private TelemetryAlertRepository telemetryAlertRepository;

    @Mock
    private ProcessedEventGuard processedEventGuard;

    @Mock
    private TelemetryIntegrationEventHandler eventHandler;

    @InjectMocks
    private TelemetryIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(processedEventGuard.firstTime(anyString())).thenReturn(true);
    }

    @Test
    void readingIsStoredOnceEvenWithDifferentEventIds() {
        DeviceProjection projection = new DeviceProjection("esp32-01", 1L, "gw-01", 12L, "Puerta", ConnectionStatus.ACTIVE);
        when(deviceProjectionRepository.findByDeviceCode("esp32-01")).thenReturn(Optional.of(projection));
        when(sensorReadingRepository.insertIgnoringDuplicates(any(SensorReading.class)))
                .thenReturn(1) // first insert succeeds
                .thenReturn(0); // second insert with same (deviceCode, capturedAt, magnitude) is ignored

        Instant capturedAt = Instant.parse("2026-10-23T15:00:00Z");
        IntegrationEnvelope env1 = new IntegrationEnvelope(
                "evt-001",
                "Reading",
                1,
                capturedAt,
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of(
                        "deviceId", "esp32-01",
                        "sensor", "temperature",
                        "value", 24.5,
                        "unit", "°C",
                        "capturedAt", capturedAt.toString(),
                        "timeSuspect", false
                )
        );

        IntegrationEnvelope env2 = new IntegrationEnvelope(
                "evt-002",
                "Reading",
                1,
                capturedAt.plusSeconds(1),
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of(
                        "deviceId", "esp32-01",
                        "sensor", "temperature",
                        "value", 24.5,
                        "unit", "°C",
                        "capturedAt", capturedAt.toString(),
                        "timeSuspect", false
                )
        );

        service.ingestReading(env1);
        service.ingestReading(env2);

        verify(sensorReadingRepository, times(2)).insertIgnoringDuplicates(any(SensorReading.class));
    }

    @Test
    void readingBeforeRegistryCreatesMinimalProjection() {
        when(deviceProjectionRepository.findByDeviceCode("esp32-unregistered")).thenReturn(Optional.empty());
        when(deviceProjectionRepository.save(any(DeviceProjection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant capturedAt = Instant.parse("2026-10-23T15:00:00Z");
        IntegrationEnvelope env = new IntegrationEnvelope(
                "evt-003",
                "Reading",
                1,
                capturedAt,
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of(
                        "deviceId", "esp32-unregistered",
                        "sensor", "humidity",
                        "value", 60.0,
                        "unit", "%",
                        "capturedAt", capturedAt.toString(),
                        "timeSuspect", false
                )
        );

        service.ingestReading(env);

        ArgumentCaptor<DeviceProjection> projectionCaptor = ArgumentCaptor.forClass(DeviceProjection.class);
        verify(deviceProjectionRepository).save(projectionCaptor.capture());
        DeviceProjection savedProjection = projectionCaptor.getValue();
        assertEquals("esp32-unregistered", savedProjection.getDeviceCode());
        assertEquals("gw-01", savedProjection.getGatewayCode());
        assertFalse(savedProjection.isDecommissioned());

        ArgumentCaptor<SensorReading> readingCaptor = ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingRepository).insertIgnoringDuplicates(readingCaptor.capture());
        SensorReading reading = readingCaptor.getValue();
        assertEquals("esp32-unregistered", reading.getDeviceCode());
        assertEquals(Magnitude.HUMIDITY, reading.getMagnitude());
        assertEquals(60.0, reading.getValue());
    }

    @Test
    void offlineStatusPublishesDeviceWentOfflineOnce() {
        DeviceProjection projection = new DeviceProjection("esp32-01", 1L, "gw-01", 12L, "Puerta", ConnectionStatus.ACTIVE);
        when(deviceProjectionRepository.findByDeviceCode("esp32-01")).thenReturn(Optional.of(projection));

        Instant now = Instant.parse("2026-10-23T15:04:05Z");
        IntegrationEnvelope envOffline1 = new IntegrationEnvelope(
                "evt-status-1",
                "DeviceStatus",
                1,
                now,
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of("deviceId", "esp32-01", "event", "offline", "lastSeen", now.toString())
        );

        service.applyDeviceStatus(envOffline1);

        assertEquals(ConnectionStatus.OFFLINE, projection.getConnectionStatus());
        verify(deviceProjectionRepository).save(projection);
        verify(eventHandler, times(1)).publishDeviceWentOffline(eq("esp32-01"), any(Instant.class), eq(12L), eq("gw-01"));

        // Second offline status for already OFFLINE device
        IntegrationEnvelope envOffline2 = new IntegrationEnvelope(
                "evt-status-2",
                "DeviceStatus",
                1,
                now.plusSeconds(30),
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of("deviceId", "esp32-01", "event", "offline", "lastSeen", now.plusSeconds(30).toString())
        );

        service.applyDeviceStatus(envOffline2);

        // Should NOT publish DeviceWentOffline again
        verify(eventHandler, times(1)).publishDeviceWentOffline(anyString(), any(Instant.class), any(), any());
    }

    @Test
    void sensorFaultRaisedPublishesSensorFaulty() {
        DeviceProjection projection = new DeviceProjection("esp32-01", 1L, "gw-01", 12L, "Puerta", ConnectionStatus.ACTIVE);
        when(deviceProjectionRepository.findByDeviceCode("esp32-01")).thenReturn(Optional.of(projection));

        Instant now = Instant.parse("2026-10-23T15:04:05Z");
        IntegrationEnvelope env = new IntegrationEnvelope(
                "evt-alert-1",
                "Alert",
                1,
                now,
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of(
                        "deviceId", "esp32-01",
                        "alert", "sensor_fault_temperature",
                        "state", "raised",
                        "sensor", "temperature"
                )
        );

        service.applyAlert(env);

        ArgumentCaptor<TelemetryAlert> alertCaptor = ArgumentCaptor.forClass(TelemetryAlert.class);
        verify(telemetryAlertRepository).save(alertCaptor.capture());
        TelemetryAlert savedAlert = alertCaptor.getValue();
        assertEquals("esp32-01", savedAlert.getDeviceCode());
        assertEquals("sensor_fault_temperature", savedAlert.getKind());
        assertEquals("RAISED", savedAlert.getState());

        verify(eventHandler).publishSensorFaulty(eq("esp32-01"), eq("temperature"), eq(12L), eq("gw-01"));
    }

    @Test
    void staleOnlineIsIgnored() {
        DeviceProjection projection = new DeviceProjection("esp32-01", 1L, "gw-01", 12L, "Puerta", ConnectionStatus.ACTIVE);
        Instant heartbeat = Instant.parse("2026-10-23T15:10:00Z");
        projection.markOnline(heartbeat);
        when(deviceProjectionRepository.findByDeviceCode("esp32-01")).thenReturn(Optional.of(projection));

        Instant staleTime = Instant.parse("2026-10-23T15:05:00Z");
        IntegrationEnvelope env = new IntegrationEnvelope(
                "evt-stale",
                "DeviceStatus",
                1,
                staleTime,
                "edge-gateway",
                null,
                "gw-01",
                null,
                Map.of("deviceId", "esp32-01", "event", "online")
        );

        service.applyDeviceStatus(env);

        // Heartbeat should remain unchanged
        assertEquals(heartbeat, projection.getLastHeartbeatAt());
        verify(deviceProjectionRepository, never()).save(projection);
    }
}
