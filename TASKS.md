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

## Task 3 — Consumidores (edge y registro de dispositivos)   [ ] pendiente
- Rama: feature/H3-ingestion
- Paso actual: pendiente
- Archivos tocados: -
- Tests: pendiente
- Último commit: -
- Métricas: -
- Bloqueos / notas: -

## Task 4 — API REST de telemetría   [ ] pendiente
- Rama: feature/H3-telemetry-api
- Paso actual: pendiente
- Archivos tocados: -
- Tests: pendiente
- Último commit: -
- Métricas: -
- Bloqueos / notas: -

## Task 5 — Cierre de H3   [ ] pendiente
- Rama: feature/H3-docs
- Paso actual: pendiente
- Archivos tocados: -
- Tests: pendiente
- Último commit: -
- Métricas: -
- Bloqueos / notas: -
