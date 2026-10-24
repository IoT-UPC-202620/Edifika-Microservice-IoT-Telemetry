package com.edifika.telemetry.domain.model.queries;

import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class GetReadingSeriesQueryTest {

    @Test
    void rawRangeOver24hIs400() {
        Instant now = Instant.now();
        Instant from = now.minus(25, ChronoUnit.HOURS);

        assertThrows(IllegalArgumentException.class, () ->
                new GetReadingSeriesQuery("esp32-01", Magnitude.TEMPERATURE, from, now, "raw")
        );
    }

    @Test
    void fromAfterToIs400() {
        Instant now = Instant.now();
        Instant from = now.plusSeconds(3600);

        assertThrows(IllegalArgumentException.class, () ->
                new GetReadingSeriesQuery("esp32-01", Magnitude.TEMPERATURE, from, now, "raw")
        );
    }

    @Test
    void bucketValidationBounds() {
        Instant now = Instant.now();
        // 1m bucket max 7 days
        Instant eightDaysAgo = now.minus(8, ChronoUnit.DAYS);
        assertThrows(IllegalArgumentException.class, () ->
                new GetReadingSeriesQuery("esp32-01", Magnitude.TEMPERATURE, eightDaysAgo, now, "1m")
        );

        // 1h bucket max 90 days
        Instant ninetyOneDaysAgo = now.minus(91, ChronoUnit.DAYS);
        assertThrows(IllegalArgumentException.class, () ->
                new GetReadingSeriesQuery("esp32-01", Magnitude.TEMPERATURE, ninetyOneDaysAgo, now, "1h")
        );

        // invalid bucket
        assertThrows(IllegalArgumentException.class, () ->
                new GetReadingSeriesQuery("esp32-01", Magnitude.TEMPERATURE, now.minusSeconds(60), now, "5m")
        );
    }
}
