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

    private final TelemetryRepository telemetryRepository;
    private final DistanceCalculator distanceCalculator;
    private final DroneRepository droneRepository;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository, DistanceCalculator distanceCalculator,
                            DroneRepository droneRepository) {
        this.telemetryRepository = telemetryRepository;
        this.distanceCalculator = distanceCalculator;
        this.droneRepository = droneRepository;
    }

    @Transactional
    public Telemetry processWithKalmanFilter(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());
        double lastLatitude = lastTelemetry.isEmpty() ? dto.getLatitude() : lastTelemetry.getFirst().getLatitude();
        double lastLongitude = lastTelemetry.isEmpty() ? dto.getLongitude() : lastTelemetry.getFirst().getLongitude();

        KalmanFilter latFilter = new KalmanFilter(lastLatitude);
        KalmanFilter lonFilter = new KalmanFilter(lastLongitude);

        double filteredLat = latFilter.update(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy());
        double filteredLon = lonFilter.update(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy());

        double totalDistance = 0.0;
        double altitudeChange = 0.0;

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

    @Transactional
    public Telemetry processWithHaversine(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        double altitudeChange = 0.0;
        double filteredLat = dto.getLatitude();
        double filteredLon = dto.getLongitude();

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());
        double totalDistance = 0.0;
        double totalDistanceHaversine = 0.0;

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

        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());
        double lastLatitude = lastTelemetry.isEmpty() ? dto.getLatitude() : lastTelemetry.getFirst().getLatitude();
        double lastLongitude = lastTelemetry.isEmpty() ? dto.getLongitude() : lastTelemetry.getFirst().getLongitude();

        KalmanFilter latFilter = new KalmanFilter(lastLatitude);
        KalmanFilter lonFilter = new KalmanFilter(lastLongitude);

        double filteredLat = latFilter.update(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy());
        double filteredLon = lonFilter.update(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy());

        double totalDistance = 0.0;
        double altitudeChange = 0.0;

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
        telemetry.setTotalDistance(totalDistance);
        telemetry.setTotalDistanceHaversine(totalDistanceHaversine);

        return telemetryRepository.save(telemetry);
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getLastTelemetry(Long droneId) {
        return telemetryRepository.findTop50ByDroneIdOrderByLocalDateTimeDesc(droneId);
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getTelemetryHistory(Long droneId, LocalDateTime start, LocalDateTime end) {
        return telemetryRepository.findByDroneIdAndLocalDateTimeBetweenOrderByLocalDateTimeAsc(droneId, start, end);
    }

    @Transactional
    public Telemetry saveRawTelemetry(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        Telemetry telemetry = new Telemetry();
        telemetry.setDrone(drone);
        telemetry.setLatitude(dto.getLatitude());
        telemetry.setLongitude(dto.getLongitude());
        telemetry.setAltitude(dto.getAltitude());
        telemetry.setSpeed(dto.getSpeed());
        telemetry.setLocalDateTime(LocalDateTime.now());

        return telemetryRepository.save(telemetry);
    }
}
