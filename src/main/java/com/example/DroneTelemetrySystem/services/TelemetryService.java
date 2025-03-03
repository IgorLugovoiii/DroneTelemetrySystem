package com.example.DroneTelemetrySystem.services;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.filters.DistanceCalculator;
import com.example.DroneTelemetrySystem.filters.KalmanFilter;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.repositories.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TelemetryService {

    private final KalmanFilter latFilter;
    private final KalmanFilter lonFilter;
    private final TelemetryRepository telemetryRepository;
    private final DistanceCalculator distanceCalculator;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository, DistanceCalculator distanceCalculator) {
        this.telemetryRepository = telemetryRepository;
        this.distanceCalculator = distanceCalculator;
        this.latFilter = new KalmanFilter(0);
        this.lonFilter = new KalmanFilter(0);
    }

    public Telemetry processWithKalmanFilter(TelemetryDto dto) {
        double filteredLat = applyKalmanFilter(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy(), latFilter);
        double filteredLon = applyKalmanFilter(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy(), lonFilter);

        return createTelemetry(dto, filteredLat, filteredLon, 0.0, 0.0); // Без обчислення відстані
    }

    public Telemetry processWithHaversine(TelemetryDto dto) {
        double filteredLat = dto.getLatitude();
        double filteredLon = dto.getLongitude();

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByOrderByTimestampDesc();
        double totalDistance = 0.0;  // Звичайна відстань (без фільтрації)
        double totalDistanceHaversine = 0.0;  // Відстань за формулою Гаверсина

        if (!lastTelemetry.isEmpty()) {
            Telemetry previousTelemetry = lastTelemetry.getFirst();
            totalDistance = previousTelemetry.getTotalDistance();
            totalDistanceHaversine = previousTelemetry.getTotalDistanceHaversine();

            double distance = distanceCalculator.calculateHaversineDistance(
                    previousTelemetry.getLatitude(), previousTelemetry.getLongitude(),
                    filteredLat, filteredLon);

            totalDistanceHaversine += distance;

            double directDistance = distanceCalculator.calculateDirectDistance(previousTelemetry, dto);
            totalDistance += directDistance;
        }

        return createTelemetry(dto, filteredLat, filteredLon, totalDistance, totalDistanceHaversine);
    }

    public Telemetry processWithKalmanAndHaversine(TelemetryDto dto) {
        double filteredLat = applyKalmanFilter(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy(), latFilter);
        double filteredLon = applyKalmanFilter(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy(), lonFilter);

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByOrderByTimestampDesc();
        double totalDistance = 0.0;

        if (!lastTelemetry.isEmpty()) {
            Telemetry previousTelemetry = lastTelemetry.getFirst();
            totalDistance = previousTelemetry.getTotalDistance();

            double distance = distanceCalculator.calculateHaversineDistance(
                    previousTelemetry.getLatitude(), previousTelemetry.getLongitude(),
                    filteredLat, filteredLon);

            totalDistance += distance;
        }

        return createTelemetry(dto, filteredLat, filteredLon, totalDistance, 0.0);
    }

    private Telemetry createTelemetry(TelemetryDto dto, double latitude, double longitude, double totalDistance,
                                      double totalDistanceHaversine) {
        Telemetry telemetry = new Telemetry();
        telemetry.setLatitude(latitude);
        telemetry.setLongitude(longitude);
        telemetry.setAltitude(dto.getAltitude());
        telemetry.setSpeed(dto.getSpeed());
        telemetry.setLocalDateTime(LocalDateTime.now());
        
        if (totalDistance == 0.0) {
            totalDistance = 0.0;
        }
        if (totalDistanceHaversine == 0.0) {
            totalDistanceHaversine = 0.0;
        }

        telemetry.setTotalDistance(totalDistance);
        telemetry.setTotalDistanceHaversine(totalDistanceHaversine);

        return telemetry;
    }

    private double applyKalmanFilter(double measurement, double speed, double gpsAccuracy, KalmanFilter filter) {
        return filter.update(measurement, speed, gpsAccuracy);
    }

    public List<Telemetry> getLastTelemetry() {
        return telemetryRepository.findTop50ByOrderByTimestampDesc();
    }
}
