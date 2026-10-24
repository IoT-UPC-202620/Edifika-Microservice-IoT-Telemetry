package com.edifika.telemetry.domain.model.aggregates;

import com.edifika.telemetry.domain.model.valueobjects.ConnectionStatus;
import com.edifika.telemetry.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "device_projections")
@Getter
@NoArgsConstructor
public class DeviceProjection extends AuditableAbstractAggregateRoot<DeviceProjection> {

    @Column(name = "device_code", unique = true, nullable = false, length = 64)
    private String deviceCode;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "gateway_code", length = 64)
    private String gatewayCode;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 32)
    private ConnectionStatus connectionStatus;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "decommissioned", nullable = false)
    private boolean decommissioned;

    public DeviceProjection(String deviceCode, String gatewayCode) {
        this.deviceCode = deviceCode;
        this.gatewayCode = gatewayCode;
        this.connectionStatus = ConnectionStatus.ACTIVE;
        this.decommissioned = false;
    }

    public DeviceProjection(String deviceCode, Long deviceId, String gatewayCode, Long buildingId, String name, ConnectionStatus connectionStatus) {
        this.deviceCode = deviceCode;
        this.deviceId = deviceId;
        this.gatewayCode = gatewayCode;
        this.buildingId = buildingId;
        this.name = name;
        this.connectionStatus = connectionStatus != null ? connectionStatus : ConnectionStatus.ACTIVE;
        this.decommissioned = false;
    }

    public void updateRegistry(Long deviceId, String gatewayCode, Long buildingId, String name, ConnectionStatus status) {
        if (deviceId != null) this.deviceId = deviceId;
        if (gatewayCode != null) this.gatewayCode = gatewayCode;
        if (buildingId != null) this.buildingId = buildingId;
        if (name != null) this.name = name;
        if (status != null) this.connectionStatus = status;
    }

    public void markOnline(Instant heartbeatAt) {
        this.connectionStatus = ConnectionStatus.ACTIVE;
        this.lastHeartbeatAt = heartbeatAt;
    }

    public void markOffline() {
        this.connectionStatus = ConnectionStatus.OFFLINE;
    }

    public void decommission() {
        this.decommissioned = true;
    }
}
