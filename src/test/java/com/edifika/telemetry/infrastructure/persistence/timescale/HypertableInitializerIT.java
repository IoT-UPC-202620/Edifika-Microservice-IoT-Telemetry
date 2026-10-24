package com.edifika.telemetry.infrastructure.persistence.timescale;

import com.edifika.telemetry.domain.model.entities.SensorReading;
import com.edifika.telemetry.domain.model.valueobjects.Magnitude;
import com.edifika.telemetry.infrastructure.persistence.jpa.repositories.SensorReadingRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HypertableInitializerIT {

    @Nested
    @SpringBootTest
    @Import(TimescaleDbTestConfig.class)
    class WithTimescaleDb {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private SensorReadingRepository sensorReadingRepository;

        @Test
        void sensorReadingsIsHypertable() {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_name = 'sensor_readings'",
                    Integer.class
            );
            assertEquals(1, count);
        }

        @Test
        @Transactional
        void insertIgnoringDuplicatesReturnsOneThenZero() {
            Instant now = Instant.parse("2026-10-24T12:00:00Z");
            SensorReading r1 = new SensorReading("esp32-01", now, Magnitude.TEMPERATURE, 23.5, "°C", false, "ev-1", 10L);
            int insertedFirst = sensorReadingRepository.insertIgnoringDuplicates(r1);
            assertEquals(1, insertedFirst);

            SensorReading r2 = new SensorReading("esp32-01", now, Magnitude.TEMPERATURE, 25.0, "°C", true, "ev-2", 10L);
            int insertedSecond = sensorReadingRepository.insertIgnoringDuplicates(r2);
            assertEquals(0, insertedSecond);
        }
    }

    @Nested
    @SpringBootTest
    @Import(PlainPostgresTestConfig.class)
    class WithPlainPostgres {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void contextStartsCleanlyWithoutTimescale() {
            Integer count = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertEquals(1, count);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TimescaleDbTestConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>(
                    DockerImageName.parse("timescale/timescaledb:latest-pg17")
                            .asCompatibleSubstituteFor("postgres")
            );
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbit() {
            return new RabbitMQContainer("rabbitmq:4-management");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PlainPostgresTestConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:17-alpine");
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbit() {
            return new RabbitMQContainer("rabbitmq:4-management");
        }
    }
}
