package com.example.DroneTelemetrySystem.services;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.filters.DistanceCalculator;
import com.example.DroneTelemetrySystem.filters.KalmanFilter;
import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.repositories.DroneRepository;
import com.example.DroneTelemetrySystem.repositories.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TelemetryService {

    private final KalmanFilter latFilter;
    private final KalmanFilter lonFilter;
    private final TelemetryRepository telemetryRepository;
    private final DistanceCalculator distanceCalculator;
    private final DroneRepository droneRepository;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository, DistanceCalculator distanceCalculator,
                            DroneRepository droneRepository) {
        this.telemetryRepository = telemetryRepository;
        this.distanceCalculator = distanceCalculator;
        this.droneRepository = droneRepository;
        this.latFilter = new KalmanFilter(0);
        this.lonFilter = new KalmanFilter(0);
    }

    @Transactional
    public Telemetry processWithKalmanFilter(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        double filteredLat = applyKalmanFilter(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy(), latFilter);
        double filteredLon = applyKalmanFilter(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy(), lonFilter);

        return createTelemetry(dto, drone, filteredLat, filteredLon, 0.0, 0.0, 0.0);
    }

    @Transactional
    public Telemetry processWithHaversine(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        double altitudeChange = 0.0;
        double filteredLat = dto.getLatitude();
        double filteredLon = dto.getLongitude();

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());
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
            altitudeChange = dto.getAltitude() - previousTelemetry.getAltitude();
        }

        return createTelemetry(dto, drone, filteredLat, filteredLon, totalDistance, totalDistanceHaversine, altitudeChange);
    }

    @Transactional
    public Telemetry processWithKalmanAndHaversine(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));
        double altitudeChange = 0.0;
        double filteredLat = applyKalmanFilter(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy(), latFilter);
        double filteredLon = applyKalmanFilter(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy(), lonFilter);

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());
        double totalDistance = 0.0;

        if (!lastTelemetry.isEmpty()) {
            Telemetry previousTelemetry = lastTelemetry.getFirst();
            totalDistance = previousTelemetry.getTotalDistance();

            double distance = distanceCalculator.calculateHaversineDistance(
                    previousTelemetry.getLatitude(), previousTelemetry.getLongitude(),
                    filteredLat, filteredLon);

            totalDistance += distance;
            altitudeChange = dto.getAltitude() - previousTelemetry.getAltitude();
        }

        return createTelemetry(dto, drone, filteredLat, filteredLon, totalDistance, 0.0, altitudeChange);
    }


    private Telemetry createTelemetry(TelemetryDto dto, Drone drone, double latitude, double longitude, double totalDistance,
                                      double totalDistanceHaversine, double altitudeChange) {
        Telemetry telemetry = new Telemetry();
        telemetry.setDrone(drone);
        telemetry.setLatitude(latitude);
        telemetry.setLongitude(longitude);
        telemetry.setAltitude(dto.getAltitude());
        telemetry.setSpeed(dto.getSpeed());
        telemetry.setLocalDateTime(LocalDateTime.now());
        telemetry.setAltitudeChange(altitudeChange);

        if (totalDistance == 0.0) {
            totalDistance = 0.0;
        }

        telemetry.setTotalDistance(totalDistance);
        telemetry.setTotalDistanceHaversine(totalDistanceHaversine);

        return telemetry;
    }

    private double applyKalmanFilter(double measurement, double speed, double gpsAccuracy, KalmanFilter filter) {
        return filter.update(measurement, speed, gpsAccuracy);
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getLastTelemetry(Long droneId) {
        return telemetryRepository.findTop50ByDroneIdOrderByLocalDateTimeDesc(droneId);
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getTelemetryHistory(Long droneId, LocalDateTime start, LocalDateTime end) {
        return telemetryRepository.findByDroneIdAndLocalDateTimeBetweenOrderByLocalDateTimeAsc(droneId, start, end);
    }
}
