package com.example.DroneTelemetrySystem.filters;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.models.Telemetry;
import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {

    // Метод для обчислення відстані між двома точками на основі формули Гаверсина
    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Радіус Землі в кілометрах

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Відстань в кілометрах
    }
    public double calculateDirectDistance(Telemetry previousTelemetry, TelemetryDto dto) {
        double lat1 = previousTelemetry.getLatitude();
        double lon1 = previousTelemetry.getLongitude();
        double lat2 = dto.getLatitude();
        double lon2 = dto.getLongitude();

        // Формула для прямої відстані (евклідова)
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }

}