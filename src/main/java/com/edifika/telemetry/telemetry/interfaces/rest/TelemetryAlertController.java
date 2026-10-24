package com.edifika.telemetry.telemetry.interfaces.rest;

import com.edifika.telemetry.domain.model.queries.GetAlertsQuery;
import com.edifika.telemetry.domain.services.TelemetryQueryService;
import com.edifika.telemetry.interfaces.rest.resources.TelemetryAlertResource;
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
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/telemetry/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry Alerts", description = "Alertas de telemetría")
public class TelemetryAlertController {

    private final TelemetryQueryService queryService;

    public TelemetryAlertController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Consultar alertas de telemetría")
    public ResponseEntity<List<TelemetryAlertResource>> getAlerts(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        GetAlertsQuery query = new GetAlertsQuery(deviceCode, state, from, to);
        List<TelemetryAlertResource> alerts = queryService.getAlerts(query);
        return ResponseEntity.ok(alerts);
    }
}
