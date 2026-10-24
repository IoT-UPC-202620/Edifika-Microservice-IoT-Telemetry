package com.edifika.telemetry.interfaces.rest;

import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import com.edifika.telemetry.domain.model.valueobjects.ConnectionStatus;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.DeviceProjectionRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.TelemetryAlertRepository;
import com.edifika.telemetry.interfaces.rest.resources.DeviceTelemetryResource;
import com.edifika.telemetry.interfaces.rest.resources.ReadingPointResource;
import com.edifika.telemetry.interfaces.rest.resources.ReadingSeriesResource;
import com.edifika.telemetry.interfaces.rest.resources.TelemetryAlertResource;
import com.edifika.telemetry.support.ContractAssertions;
import com.edifika.telemetry.support.JwtTestTokens;
import com.edifika.telemetry.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers
class TelemetryReadingControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private DeviceProjectionRepository deviceProjectionRepository;

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private TelemetryAlertRepository telemetryAlertRepository;

    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
    }

    private HttpHeaders residentHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestTokens.resident(15L));
        return headers;
    }

    @Test
    void rawRangeOver24hIs400() {
        HttpEntity<?> entity = new HttpEntity<>(residentHeaders());
        String url = "http://localhost:" + port + "/api/v1/telemetry/readings" +
                "?deviceCode=esp32-raw-err&magnitude=TEMPERATURE&bucket=raw" +
                "&from=2026-10-20T10:00:00Z&to=2026-10-21T12:00:00Z";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void fromAfterToIs400() {
        HttpEntity<?> entity = new HttpEntity<>(residentHeaders());
        String url = "http://localhost:" + port + "/api/v1/telemetry/readings" +
                "?deviceCode=esp32-from-to&magnitude=TEMPERATURE" +
                "&from=2026-10-23T15:00:00Z&to=2026-10-23T14:00:00Z";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void oneMinuteBucketsAggregate() {
        String deviceCode = "esp32-agg-01";
        deviceProjectionRepository.save(new DeviceProjection(
                deviceCode, 50L, "gw-01", 12L, "Piscina", ConnectionStatus.ACTIVE
        ));

        Instant base = Instant.parse("2026-10-23T15:04:00Z");
        for (int i = 0; i < 6; i++) {
            sensorReadingRepository.insertIgnoringDuplicates(new SensorReading(
                    deviceCode,
                    base.plusSeconds(i * 10),
                    Magnitude.TEMPERATURE,
                    20.0 + (i * 2.0), // 20, 22, 24, 26, 28, 30 -> avg = 25, min = 20, max = 30
                    "°C",
                    false,
                    "evt-" + i,
                    12L
            ));
        }

        HttpEntity<?> entity = new HttpEntity<>(residentHeaders());
        String url = "http://localhost:" + port + "/api/v1/telemetry/readings" +
                "?deviceCode=" + deviceCode + "&magnitude=TEMPERATURE&bucket=1m" +
                "&from=2026-10-23T15:00:00Z&to=2026-10-23T15:10:00Z";

        ResponseEntity<ReadingSeriesResource> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ReadingSeriesResource.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReadingSeriesResource series = response.getBody();
        assertNotNull(series);
        assertEquals("1m", series.bucket());
        assertEquals(Magnitude.TEMPERATURE.name(), series.magnitude());
        assertEquals("°C", series.unit());
        assertEquals(1, series.points().size());

        ReadingPointResource pt = series.points().get(0);
        assertEquals(6L, pt.count());
        assertEquals(20.0, pt.min());
        assertEquals(30.0, pt.max());
        assertEquals(25.0, pt.value());
    }

    @Test
    void latestPrefersNonSuspectReadings() {
        String deviceCode = "esp32-suspect-01";
        deviceProjectionRepository.save(new DeviceProjection(
                deviceCode, 51L, "gw-01", 12L, "Piscina", ConnectionStatus.ACTIVE
        ));

        // Older non-suspect reading
        sensorReadingRepository.insertIgnoringDuplicates(new SensorReading(
                deviceCode,
                Instant.parse("2026-10-23T15:00:00Z"),
                Magnitude.TEMPERATURE,
                23.5,
                "°C",
                false,
                "evt-good",
                12L
        ));

        // Newer suspect reading
        sensorReadingRepository.insertIgnoringDuplicates(new SensorReading(
                deviceCode,
                Instant.parse("2026-10-23T15:05:00Z"),
                Magnitude.TEMPERATURE,
                99.9,
                "°C",
                true,
                "evt-suspect",
                12L
        ));

        HttpEntity<?> entity = new HttpEntity<>(residentHeaders());
        String url = "http://localhost:" + port + "/api/v1/telemetry/devices/" + deviceCode + "/latest";

        ResponseEntity<DeviceTelemetryResource> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, DeviceTelemetryResource.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DeviceTelemetryResource res = response.getBody();
        assertNotNull(res);
        assertFalse(res.latest().isEmpty());
        assertEquals(23.5, res.latest().get(0).value(), "Must prefer non-suspect reading over suspect reading");
    }

    @Test
    void responsesMatchContract() {
        String deviceCode = "esp32-spec-01";
        deviceProjectionRepository.save(new DeviceProjection(
                deviceCode, 52L, "gw-01", 12L, "Spec Device", ConnectionStatus.ACTIVE
        ));
        sensorReadingRepository.insertIgnoringDuplicates(new SensorReading(
                deviceCode,
                Instant.parse("2026-10-23T15:00:00Z"),
                Magnitude.TEMPERATURE,
                24.0,
                "°C",
                false,
                "evt-spec-1",
                12L
        ));
        telemetryAlertRepository.save(new TelemetryAlert(
                deviceCode,
                "sensor_fault_temperature",
                "RAISED",
                "{\"sensor\":\"temperature\"}",
                Instant.parse("2026-10-23T15:00:00Z")
        ));

        HttpEntity<?> entity = new HttpEntity<>(residentHeaders());

        // 1. DeviceTelemetryResource
        ResponseEntity<DeviceTelemetryResource> devRes = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/telemetry/devices/" + deviceCode + "/latest",
                HttpMethod.GET, entity, DeviceTelemetryResource.class
        );
        assertEquals(HttpStatus.OK, devRes.getStatusCode());
        ContractAssertions.assertMatches("contracts/schemas/rest/telemetry/DeviceTelemetryResource.schema.json", devRes.getBody());

        // 2. ReadingSeriesResource
        ResponseEntity<ReadingSeriesResource> seriesRes = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/telemetry/readings?deviceCode=" + deviceCode +
                        "&magnitude=TEMPERATURE&from=2026-10-23T14:00:00Z&to=2026-10-23T16:00:00Z&bucket=raw",
                HttpMethod.GET, entity, ReadingSeriesResource.class
        );
        assertEquals(HttpStatus.OK, seriesRes.getStatusCode());
        ContractAssertions.assertMatches("contracts/schemas/rest/telemetry/ReadingSeriesResource.schema.json", seriesRes.getBody());

        // 3. TelemetryAlertResource
        ResponseEntity<List<TelemetryAlertResource>> alertRes = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/telemetry/alerts?deviceCode=" + deviceCode,
                HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, alertRes.getStatusCode());
        assertNotNull(alertRes.getBody());
        assertFalse(alertRes.getBody().isEmpty());
        ContractAssertions.assertMatches("contracts/schemas/rest/telemetry/TelemetryAlertResource.schema.json", alertRes.getBody().get(0));
    }

    @Test
    void anyAuthenticatedUserCanRead() {
        // Unauthenticated request should be 401
        ResponseEntity<String> unauth = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/telemetry/devices",
                HttpMethod.GET, HttpEntity.EMPTY, String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, unauth.getStatusCode());

        // Authenticated resident request should be 200
        HttpHeaders resident = new HttpHeaders();
        resident.setBearerAuth(JwtTestTokens.resident(42L));
        HttpEntity<?> entity = new HttpEntity<>(resident);

        ResponseEntity<List<DeviceTelemetryResource>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/telemetry/devices",
                HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
