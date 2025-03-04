package com.example.DroneTelemetrySystem.dtos;

import lombok.Data;

@Data
public class TelemetryDto {
    private Long id;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private double gpsAccuracy;
    private Long droneId;
    private double altitudeChange;

    public TelemetryDto(Long id, double latitude, double longitude, double altitude, double speed,
                        double gpsAccuracy, Long droneId, double altitudeChange) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.gpsAccuracy = gpsAccuracy;
        this.droneId = droneId;
        this.altitudeChange = altitudeChange;
    }
}
