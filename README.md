# Edifika — IoT Telemetry Service (H3)

Microservicio de telemetría IoT del ecosistema Edifika. Ingiere de forma idempotente lecturas de temperatura, humedad, alertas y estado de nodos desde edge gateways; almacena las lecturas en PostgreSQL / TimescaleDB con particionamiento temporal; proyecta dispositivos registrados por el servicio de acceso; y expone endpoints REST seguros para consulta de series crudas y agregadas.

---

## Características Principales

- **Ingesta Idempotente**: Deduplicación en base de datos (`ON CONFLICT (device_code, captured_at, magnitude) DO NOTHING`) y guard en memoria/BD (`processed_events`).
- **TimescaleDB Hypertable**: `sensor_readings` particionada temporalmente por `captured_at` de forma automática al arrancar (con degradación limpia si no existe la extensión TimescaleDB).
- **Mensajería Resiliente**: RabbitMQ con retry exponencial (1s, 2s, 4s), descarte a Dead Letter Queue (`edifika.dlx`) y captura de payloads malformados sin requeue infinito.
- **Eventos de Dominio Salientes**: Publicación de `telemetry.device-went-offline` y `telemetry.sensor-faulty` en cumplimiento con los esquemas JSON de contratos.
- **Consultas Agregadas**: Agregación nativa SQL (`date_trunc`) con buckets `raw`, `1m` y `1h` (máx. 5000 puntos).
- **Filtro de Lecturas Sospechosas**: El endpoint `latest` prioriza lecturas con `timeSuspect = false`.
- **Seguridad**: Autenticación JWT HS256 vía filtro de seguridad (acceso de lectura a cualquier usuario autenticado).

---

## Variables de Entorno

| Variable | Descripción | Valor por Defecto |
|---|---|---|
| `PORT` | Puerto HTTP del servicio | `8090` |
| `DATABASE_URL` | URL JDBC de conexión a PostgreSQL / TimescaleDB | `jdbc:postgresql://localhost:5432/edifika_telemetry` |
| `DATABASE_USERNAME` | Usuario de base de datos | `edifika` |
| `DATABASE_PASSWORD` | Contraseña de base de datos | `edifika_secret` |
| `JWT_SECRET` | Clave secreta HMAC para validación de tokens JWT | — |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para CORS (separados por coma) | `*` |
| `RABBITMQ_HOST` | Host del broker RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Puerto AMQP de RabbitMQ | `5672` |
| `RABBITMQ_USERNAME` | Usuario RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Contraseña RabbitMQ | `guest` |
| `RABBITMQ_VHOST` | Virtual host RabbitMQ | `/` |
| `RABBITMQ_SSL` | Habilitar conexión SSL con RabbitMQ | `false` |

---

## Topología RabbitMQ

### Exchanges
- `edifika.events` (Topic, Durable)
- `edifika.edge.commands` (Topic, Durable)
- `edifika.dlx` (Topic, Durable)

### Colas y Enlaces
| Cola | Exchange | Routing Keys | DLQ Asociada |
|---|---|---|---|
| `telemetry.edge-events` | `edifika.events` | `edge.reading`, `edge.alert`, `edge.device-status` | `telemetry.edge-events.dlq` |
| `telemetry.device-registry` | `edifika.events` | `iot.device-registered`, `iot.device-updated`, `iot.device-decommissioned` | `telemetry.device-registry.dlq` |

### Eventos Salientes (`edifika.events`)
| Routing Key | Event Type | Payload |
|---|---|---|
| `telemetry.device-went-offline` | `DeviceWentOffline` | `{"deviceCode":"...", "lastSeenAt":"..."}` |
| `telemetry.sensor-faulty` | `SensorFaulty` | `{"deviceCode":"...", "sensor":"temperature"\|"humidity"}` |

---

## API REST

Base URL: `/api/v1/telemetry` (requiere cabecera `Authorization: Bearer <JWT>`).

| Método | Endpoint | Parámetros | Descripción |
|---|---|---|---|
| `GET` | `/devices` | `?buildingId=` | Lista dispositivos proyectados con su última lectura por magnitud |
| `GET` | `/devices/{deviceCode}/latest` | — | Detalle y última lectura de un dispositivo (404 si no existe) |
| `GET` | `/readings` | `deviceCode` (req), `magnitude` (`TEMPERATURE`\|`HUMIDITY`, req), `from`?, `to`?, `bucket` (`raw`\|`1m`\|`1h`) | Serie temporal agregada o cruda (400 si `from >= to` o rango excedido) |
| `GET` | `/alerts` | `?deviceCode=&state=RAISED\|CLEARED&from=&to=` | Historial de alertas de telemetría (máx. 500) |
| `GET` | `/v3/api-docs` | — | Especificación OpenAPI 3 (público) |
| `GET` | `/swagger-ui.html` | — | Interfaz gráfica Swagger UI (público) |

---

## Comandos de Construcción y Test

### Ejecutar Suite Completa de Tests (Unitarios e Integración)
Los tests de integración usan Testcontainers (`timescale/timescaledb:latest-pg17` y `rabbitmq:4-management`) en puertos dinámicos aislados:
```bash
.\mvnw.cmd -B verify
```

### Ejecutar Solo Tests Unitarios
```bash
.\mvnw.cmd test
```

### Ejecutar Test de Integración Específico
```bash
.\mvnw.cmd -B verify "-Dit.test=EdgeTelemetryConsumerIT"
```

---

## Construcción de Imagen Docker

```bash
docker build -t edifika/iot-telemetry:0.1.0 .
```
El contenedor expone el puerto `8090`.
