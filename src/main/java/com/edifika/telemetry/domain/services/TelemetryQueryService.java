package com.edifika.telemetry.domain.services;

import com.edifika.telemetry.domain.model.queries.GetAlertsQuery;
import com.edifika.telemetry.domain.model.queries.GetLatestReadingsQuery;
import com.edifika.telemetry.domain.model.queries.GetReadingSeriesQuery;
import com.edifika.telemetry.domain.model.queries.GetTelemetryDevicesQuery;
import com.edifika.telemetry.interfaces.rest.resources.DeviceTelemetryResource;
import com.edifika.telemetry.interfaces.rest.resources.ReadingSeriesResource;
import com.edifika.telemetry.interfaces.rest.resources.TelemetryAlertResource;

import java.util.List;

public interface TelemetryQueryService {
    List<DeviceTelemetryResource> getDevices(GetTelemetryDevicesQuery query);
    DeviceTelemetryResource getLatestByDeviceCode(GetLatestReadingsQuery query);
    ReadingSeriesResource getReadingSeries(GetReadingSeriesQuery query);
    List<TelemetryAlertResource> getAlerts(GetAlertsQuery query);
}
