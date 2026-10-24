package com.edifika.telemetry.telemetry.interfaces.rest;

import com.edifika.telemetry.domain.model.queries.GetLatestReadingsQuery;
import com.edifika.telemetry.domain.model.queries.GetTelemetryDevicesQuery;
import com.edifika.telemetry.domain.services.TelemetryQueryService;
import com.edifika.telemetry.interfaces.rest.resources.DeviceTelemetryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/telemetry/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry Devices", description = "Dispositivos y última telemetría")
public class TelemetryDeviceController {

    private final TelemetryQueryService queryService;

    public TelemetryDeviceController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Listar dispositivos de telemetría")
    public ResponseEntity<List<DeviceTelemetryResource>> getDevices(@RequestParam(required = false) Long buildingId) {
        List<DeviceTelemetryResource> resources = queryService.getDevices(new GetTelemetryDevicesQuery(buildingId));
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{deviceCode}/latest")
    @Operation(summary = "Obtener última lectura de un dispositivo")
    public ResponseEntity<DeviceTelemetryResource> getLatest(@PathVariable String deviceCode) {
        DeviceTelemetryResource resource = queryService.getLatestByDeviceCode(new GetLatestReadingsQuery(deviceCode));
        return ResponseEntity.ok(resource);
    }
}
