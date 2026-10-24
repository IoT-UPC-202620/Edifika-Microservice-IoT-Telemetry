package com.edifika.telemetry.application.internal.queryservices;

import com.edifika.telemetry.domain.model.aggregates.DeviceProjection;
import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.entities.TelemetryAlert;
import com.edifika.telemetry.domain.model.queries.GetAlertsQuery;
import com.edifika.telemetry.domain.model.queries.GetLatestReadingsQuery;
import com.edifika.telemetry.domain.model.queries.GetReadingSeriesQuery;
import com.edifika.telemetry.domain.model.queries.GetTelemetryDevicesQuery;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.domain.services.TelemetryQueryService;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.DeviceProjectionRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.TelemetryAlertRepository;
import com.edifika.telemetry.interfaces.rest.resources.DeviceTelemetryResource;
import com.edifika.telemetry.interfaces.rest.resources.LatestReadingResource;
import com.edifika.telemetry.interfaces.rest.resources.ReadingPointResource;
import com.edifika.telemetry.interfaces.rest.resources.ReadingSeriesResource;
import com.edifika.telemetry.interfaces.rest.resources.TelemetryAlertResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class TelemetryQueryServiceImpl implements TelemetryQueryService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DeviceProjectionRepository deviceProjectionRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final TelemetryAlertRepository telemetryAlertRepository;

    public TelemetryQueryServiceImpl(
            DeviceProjectionRepository deviceProjectionRepository,
            SensorReadingRepository sensorReadingRepository,
            TelemetryAlertRepository telemetryAlertRepository
    ) {
        this.deviceProjectionRepository = deviceProjectionRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.telemetryAlertRepository = telemetryAlertRepository;
    }

    @Override
    public List<DeviceTelemetryResource> getDevices(GetTelemetryDevicesQuery query) {
        List<DeviceProjection> projections;
        if (query.buildingId() != null) {
            projections = deviceProjectionRepository.findByBuildingId(query.buildingId());
        } else {
            projections = deviceProjectionRepository.findAll();
        }

        return projections.stream()
                .filter(p -> !p.isDecommissioned())
                .map(this::mapToDeviceTelemetryResource)
                .toList();
    }

    @Override
    public DeviceTelemetryResource getLatestByDeviceCode(GetLatestReadingsQuery query) {
        DeviceProjection projection = deviceProjectionRepository.findByDeviceCode(query.deviceCode())
                .orElseThrow(() -> new NoSuchElementException("Device not found with code: " + query.deviceCode()));

        return mapToDeviceTelemetryResource(projection);
    }

    private DeviceTelemetryResource mapToDeviceTelemetryResource(DeviceProjection projection) {
        List<LatestReadingResource> latestList = new ArrayList<>();
        for (Magnitude mag : Magnitude.values()) {
            Optional<SensorReading> latestOpt = sensorReadingRepository
                    .findFirstByDeviceCodeAndMagnitudeAndTimeSuspectFalseOrderByCapturedAtDesc(projection.getDeviceCode(), mag)
                    .or(() -> sensorReadingRepository.findFirstByDeviceCodeAndMagnitudeOrderByCapturedAtDesc(projection.getDeviceCode(), mag));

            latestOpt.ifPresent(r -> latestList.add(new LatestReadingResource(
                    r.getMagnitude().name(),
                    r.getValue(),
                    r.getUnit(),
                    r.getCapturedAt()
            )));
        }

        return new DeviceTelemetryResource(
                projection.getDeviceCode(),
                projection.getDeviceId() != null ? projection.getDeviceId() : 0L,
                projection.getName() != null ? projection.getName() : projection.getDeviceCode(),
                projection.getBuildingId() != null ? projection.getBuildingId() : 0L,
                projection.getConnectionStatus().name(),
                projection.getLastHeartbeatAt(),
                latestList
        );
    }

    @Override
    public ReadingSeriesResource getReadingSeries(GetReadingSeriesQuery query) {
        String unitStr = query.magnitude() == Magnitude.TEMPERATURE ? "°C" : "%";
        List<ReadingPointResource> points = new ArrayList<>();

        if ("raw".equalsIgnoreCase(query.bucket())) {
            List<SensorReading> readings = sensorReadingRepository
                    .findByDeviceCodeAndMagnitudeAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
                            query.deviceCode(), query.magnitude(), query.from(), query.to()
                    );
            for (SensorReading r : readings) {
                points.add(new ReadingPointResource(
                        r.getCapturedAt(),
                        r.getValue(),
                        r.getValue(),
                        r.getValue(),
                        1L
                ));
            }
        } else {
            String dbUnit = "1m".equalsIgnoreCase(query.bucket()) ? "minute" : "hour";
            List<SensorReadingRepository.BucketRow> rows = sensorReadingRepository.aggregateReadings(
                    query.deviceCode(), query.magnitude().name(), query.from(), query.to(), dbUnit
            );
            // Reverse / sort ascending by time
            rows.stream()
                    .sorted(Comparator.comparing(SensorReadingRepository.BucketRow::getT))
                    .forEach(row -> points.add(new ReadingPointResource(
                            row.getT(),
                            Math.round(row.getValue() * 100.0) / 100.0,
                            Math.round(row.getMin() * 100.0) / 100.0,
                            Math.round(row.getMax() * 100.0) / 100.0,
                            row.getCount()
                    )));
        }

        return new ReadingSeriesResource(
                query.deviceCode(),
                query.magnitude().name(),
                unitStr,
                query.bucket(),
                query.from(),
                query.to(),
                points
        );
    }

    @Override
    public List<TelemetryAlertResource> getAlerts(GetAlertsQuery query) {
        List<TelemetryAlert> alerts;
        if (query.deviceCode() != null && query.state() != null) {
            alerts = telemetryAlertRepository.findByDeviceCodeAndStateOrderByOccurredAtDesc(
                    query.deviceCode(), query.state().toUpperCase()
            );
        } else if (query.deviceCode() != null) {
            alerts = telemetryAlertRepository.findByDeviceCodeOrderByOccurredAtDesc(query.deviceCode());
        } else if (query.state() != null) {
            alerts = telemetryAlertRepository.findByStateOrderByOccurredAtDesc(query.state().toUpperCase());
        } else if (query.from() != null && query.to() != null) {
            alerts = telemetryAlertRepository.findByOccurredAtBetweenOrderByOccurredAtDesc(query.from(), query.to());
        } else {
            alerts = telemetryAlertRepository.findAllByOrderByOccurredAtDesc();
        }

        return alerts.stream()
                .limit(500)
                .map(a -> {
                    Map<String, Object> detailsMap;
                    try {
                        detailsMap = MAPPER.readValue(a.getDetails(), new TypeReference<>() {});
                    } catch (Exception e) {
                        detailsMap = Map.of();
                    }
                    return new TelemetryAlertResource(
                            a.getId(),
                            a.getDeviceCode(),
                            a.getKind(),
                            a.getState(),
                            detailsMap,
                            a.getOccurredAt()
                    );
                })
                .toList();
    }
}
