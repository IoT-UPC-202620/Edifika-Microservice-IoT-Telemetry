package com.edifika.telemetry.telemetry.interfaces.rest;

import com.edifika.telemetry.domain.model.queries.GetReadingSeriesQuery;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.domain.services.TelemetryQueryService;
import com.edifika.telemetry.interfaces.rest.resources.ReadingSeriesResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(value = "/api/v1/telemetry/readings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry Readings", description = "Series de lecturas de sensores")
public class TelemetryReadingController {

    private final TelemetryQueryService queryService;

    public TelemetryReadingController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Consultar series de lecturas agregadas o crudas")
    public ResponseEntity<ReadingSeriesResource> getReadings(
            @RequestParam String deviceCode,
            @RequestParam String magnitude,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "raw") String bucket
    ) {
        if (magnitude == null || magnitude.isBlank()) {
            throw new IllegalArgumentException("magnitude is required");
        }
        Magnitude mag;
        try {
            mag = Magnitude.valueOf(magnitude.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid magnitude: " + magnitude + ". Allowed: TEMPERATURE, HUMIDITY");
        }

        GetReadingSeriesQuery query = new GetReadingSeriesQuery(deviceCode, mag, from, to, bucket);
        ReadingSeriesResource series = queryService.getReadingSeries(query);
        return ResponseEntity.ok(series);
    }
}
