package com.edifika.telemetry.telemetry.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Schema(description = "Respuesta de error")
public record ErrorResource(
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Mensaje legible en español") String message,
        Instant timestamp
) {
    public static ErrorResource of(HttpStatus status, String message) {
        return new ErrorResource(status.value(), status.getReasonPhrase(), message, Instant.now());
    }
}
