package com.example.DroneTelemetrySystem.dtos;

import lombok.Data;

@Data
public class TelemetryDto {
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private double gpsAccuracy;
    private Long droneId;
    private double altitudeChange;
}
