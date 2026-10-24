# TASKS — IoT Telemetry Service (H3)

## Task 1 — Esqueleto   [x] hecha
- Rama: feature/H3-scaffold
- Paso actual: Completado
- Archivos tocados: pom.xml, Dockerfile, .env.example, src/main/java/com/edifika/telemetry/..., src/test/java/com/edifika/telemetry/...
- Tests: .\mvnw.cmd -B verify "-Dit.test=ApplicationContextIT" -> PASS (2 tests run, 0 failures, 0 errors)
- Último commit: chore: scaffold IoT Telemetry from Payment template (H3)
- Métricas: Contexto arriba con Testcontainers (Postgres / TimescaleDB y RabbitMQ), 401 en /api/v1/telemetry/devices, 200 en /v3/api-docs
- Bloqueos / notas: ninguno

## Task 2 — Modelo de datos e hypertable   [x] hecha
- Rama: feature/H3-readings-model
- Paso actual: Completado
- Archivos tocados: src/main/java/com/edifika/telemetry/domain/model/..., src/main/java/com/edifika/telemetry/domain/repositories/..., src/main/java/com/edifika/telemetry/application/internal/messaging/..., src/main/java/com/edifika/telemetry/infrastructure/persistence/timescale/..., src/test/java/com/edifika/telemetry/infrastructure/persistence/HypertableInitializerIT.java
- Tests: .\mvnw.cmd -B verify "-Dit.test=HypertableInitializerIT" -> PASS (3 tests run, 0 failures, 0 errors)
- Último commit: feat(model): sensor readings with Timescale hypertable, device projection and alerts (H3)
- Métricas: Hypertable sensor_readings particionada por captured_at; deduplicación ON CONFLICT (device_code, captured_at, magnitude) DO NOTHING; fallback limpio en Postgres sin extensión TimescaleDB.
- Bloqueos / notas: ninguno

## Task 3 — Consumidores (edge y registro de dispositivos)   [x] hecha
- Rama: feature/H3-ingestion
- Paso actual: Completado
- Archivos tocados: pom.xml, src/main/java/com/edifika/telemetry/domain/services/..., src/main/java/com/edifika/telemetry/application/internal/commandservices/..., src/main/java/com/edifika/telemetry/application/internal/eventhandlers/..., src/main/java/com/edifika/telemetry/infrastructure/messaging/rabbitmq/..., src/main/java/com/edifika/telemetry/interfaces/messaging/..., src/test/java/com/edifika/telemetry/application/internal/commandservices/..., src/test/java/com/edifika/telemetry/interfaces/messaging/..., src/test/java/com/edifika/telemetry/support/ContractAssertions.java
- Tests: .\mvnw.cmd -B verify -> PASS (8 tests run, 0 failures, 0 errors)
- Último commit: feat(ingestion): idempotent readings, alerts, device status and registry projection (H3)
- Métricas: Ingestión de 100 lecturas en 1259 ms (~12.6 ms/lectura, p95 < 500 ms); deduplicación exacta al reenviar (100 filas intactas); publicación de eventos de dominio DeviceWentOffline y SensorFaulty validados contra schemas; enrutamiento a DLQ para mensajes malformados.
- Bloqueos / notas: ninguno

## Task 4 — API REST de telemetría   [x] hecha
- Rama: feature/H3-telemetry-api
- Paso actual: Completado
- Archivos tocados: src/main/java/com/edifika/telemetry/domain/model/queries/..., src/main/java/com/edifika/telemetry/interfaces/rest/resources/..., src/main/java/com/edifika/telemetry/domain/services/TelemetryQueryService.java, src/main/java/com/edifika/telemetry/application/internal/queryservices/TelemetryQueryServiceImpl.java, src/main/java/com/edifika/telemetry/telemetry/interfaces/rest/..., src/main/java/com/edifika/telemetry/infrastructure/persistence/jpa/repositories/SensorReadingRepository.java, src/test/java/com/edifika/telemetry/domain/model/queries/GetReadingSeriesQueryTest.java, src/test/java/com/edifika/telemetry/interfaces/rest/TelemetryReadingControllerIT.java
- Tests: .\mvnw.cmd -B verify -> PASS (14 tests run, 0 failures, 0 errors)
- Último commit: feat(api): telemetry devices, latest, bucketed series and alerts (H3)
- Métricas: Validación de rangos (raw 24h, 1m 7d, 1h 90d, from < to); agregación en SQL date_trunc (6 lecturas en 1 minuto agregadas a 1 punto); latest con preferencia por lecturas non-suspect; respuestas REST validadas 100% contra JSON schemas de contratos; acceso seguro para cualquier usuario autenticado (401 sin token, 200 con residente).
- Bloqueos / notas: ninguno

## Task 5 — Cierre de H3   [ ] pendiente
- Rama: feature/H3-docs
- Paso actual: pendiente
- Archivos tocados: -
- Tests: pendiente
- Último commit: -
- Métricas: -
- Bloqueos / notas: -
