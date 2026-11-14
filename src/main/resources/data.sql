-- Datos de prueba locales (solo corre con SEED_MODE=always). Idempotente.
-- Mismos dispositivos que IoT-Access-Management. El firmware (DHT11) publica "temperature" (°C) y "humidity" (%).

INSERT INTO device_projections (id, created_at, updated_at, building_id, connection_status, decommissioned, device_code, device_id, gateway_code, last_heartbeat_at, name) VALUES
  (1, now(), now(), 1, 'ACTIVE',  false, 'esp32-acc-01',   1, 'edge-gateway-01', now(),                     'Puerta principal'),
  (2, now(), now(), 1, 'ACTIVE',  false, 'esp32-gym-01',   2, 'edge-gateway-01', now(),                     'Puerta gimnasio'),
  (3, now(), now(), 2, 'OFFLINE', false, 'esp32-tower-01', 3, 'edge-gateway-02', now() - interval '6 hours', 'Puerta lobby')
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('device_projections', 'id'), (SELECT MAX(id) FROM device_projections));

-- Lecturas de las ultimas 48 h cada 15 min; se regeneran en cada arranque (source_event_id seed-*).
-- Ciclo diario de Lima en noviembre: maximo de temperatura ~15:00, humedad inversa (65-85 %).
-- esp32-tower-01 deja de reportar hace 6 h (coherente con su estado OFFLINE).
DELETE FROM sensor_readings WHERE source_event_id LIKE 'seed-%';

INSERT INTO sensor_readings (captured_at, device_code, magnitude, building_id, source_event_id, time_suspect, unit, value)
SELECT s.t, d.code, 'TEMPERATURE', d.building, 'seed-' || d.code || '-T-' || extract(epoch FROM s.t)::bigint, false, '°C',
       round((21.0 + d.temp_off
              + 2.5 * sin(2 * pi() * (extract(hour FROM s.t AT TIME ZONE 'America/Lima')
                                      + extract(minute FROM s.t AT TIME ZONE 'America/Lima') / 60.0 - 9) / 24)
              + (random() - 0.5) * 0.6)::numeric, 1)
FROM generate_series(date_trunc('hour', now()) - interval '48 hours', now(), interval '15 minutes') AS s(t)
CROSS JOIN (VALUES ('esp32-acc-01', 1, 0.0, 0.0, interval '0'), ('esp32-gym-01', 1, 1.5, 4.0, interval '0'), ('esp32-tower-01', 2, -0.8, -3.0, interval '6 hours'))
  AS d(code, building, temp_off, hum_off, silent)
WHERE s.t <= now() - d.silent
ON CONFLICT DO NOTHING;

INSERT INTO sensor_readings (captured_at, device_code, magnitude, building_id, source_event_id, time_suspect, unit, value)
SELECT s.t, d.code, 'HUMIDITY', d.building, 'seed-' || d.code || '-H-' || extract(epoch FROM s.t)::bigint, false, '%',
       round(LEAST(95, GREATEST(40, 75.0 + d.hum_off
              - 9.0 * sin(2 * pi() * (extract(hour FROM s.t AT TIME ZONE 'America/Lima')
                                      + extract(minute FROM s.t AT TIME ZONE 'America/Lima') / 60.0 - 9) / 24)
              + (random() - 0.5) * 2.0))::numeric, 1)
FROM generate_series(date_trunc('hour', now()) - interval '48 hours', now(), interval '15 minutes') AS s(t)
CROSS JOIN (VALUES ('esp32-acc-01', 1, 0.0, 0.0, interval '0'), ('esp32-gym-01', 1, 1.5, 4.0, interval '0'), ('esp32-tower-01', 2, -0.8, -3.0, interval '6 hours'))
  AS d(code, building, temp_off, hum_off, silent)
WHERE s.t <= now() - d.silent
ON CONFLICT DO NOTHING;

-- Alertas (mismos "kind" que emite el Edge Gateway); tambien se regeneran
DELETE FROM telemetry_alerts WHERE details LIKE '%"seed":true%';
INSERT INTO telemetry_alerts (details, device_code, kind, occurred_at, state) VALUES
  ('{"seed":true,"message":"Sin heartbeat del nodo"}',                 'esp32-tower-01', 'device_offline',            now() - interval '6 hours',  'RAISED'),
  ('{"seed":true,"sensor":"temperature","message":"Lectura fuera de rango"}', 'esp32-gym-01', 'sensor_fault_temperature', now() - interval '20 hours', 'RAISED'),
  ('{"seed":true,"sensor":"temperature","message":"Sensor recuperado"}',     'esp32-gym-01', 'sensor_fault_temperature', now() - interval '19 hours', 'CLEARED'),
  ('{"seed":true,"value":88.5,"message":"Humedad alta en gimnasio"}',  'esp32-gym-01',   'humidity_high',             now() - interval '30 hours', 'CLEARED');
