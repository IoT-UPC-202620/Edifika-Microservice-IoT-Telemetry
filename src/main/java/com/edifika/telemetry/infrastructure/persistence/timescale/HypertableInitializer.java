package com.edifika.telemetry.infrastructure.persistence.timescale;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HypertableInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public HypertableInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_extension WHERE extname = 'timescaledb'",
                    Integer.class
            );
            if (count != null && count > 0) {
                log.info("TimescaleDB extension detected, converting sensor_readings to hypertable if not exists...");
                jdbcTemplate.execute(
                        "SELECT create_hypertable('sensor_readings', 'captured_at', if_not_exists => TRUE, migrate_data => TRUE)"
                );
                log.info("Hypertable sensor_readings created or already exists.");
            } else {
                log.info("TimescaleDB extension not present; sensor_readings runs as standard PostgreSQL table.");
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize TimescaleDB hypertable for sensor_readings: {}", ex.getMessage());
        }
    }
}
