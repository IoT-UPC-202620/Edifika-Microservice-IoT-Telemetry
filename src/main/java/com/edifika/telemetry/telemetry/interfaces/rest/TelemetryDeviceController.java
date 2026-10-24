package com.edifika.telemetry.telemetry.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/telemetry/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Telemetry Devices", description = "Dispositivos y última telemetría")
public class TelemetryDeviceController {

    @GetMapping
    @Operation(summary = "Listar dispositivos de telemetría")
    public ResponseEntity<List<Object>> getDevices(@RequestParam(required = false) Long buildingId) {
        return ResponseEntity.ok(List.of());
    }
}
