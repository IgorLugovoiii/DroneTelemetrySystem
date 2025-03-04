package com.example.DroneTelemetrySystem.services;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.filters.DistanceCalculator;
import com.example.DroneTelemetrySystem.filters.KalmanFilter;
import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.models.RawTelemetry;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.repositories.DroneRepository;
import com.example.DroneTelemetrySystem.repositories.RawTelemetryRepository;
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
    private final RawTelemetryRepository rawTelemetryRepository;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository, DistanceCalculator distanceCalculator,
                            DroneRepository droneRepository, RawTelemetryRepository rawTelemetryRepository) {
        this.telemetryRepository = telemetryRepository;
        this.distanceCalculator = distanceCalculator;
        this.droneRepository = droneRepository;
        this.rawTelemetryRepository = rawTelemetryRepository;
    }

    @Transactional
    public RawTelemetry saveRawTelemetry(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        double altitudeChange = 0.0;
        double totalDistance = 0.0;
        double totalDistanceHaversine = 0.0;

        List<RawTelemetry> lastRawTelemetry = rawTelemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());

        if (!lastRawTelemetry.isEmpty()) {
            RawTelemetry previousRaw = lastRawTelemetry.getFirst();
            altitudeChange = dto.getAltitude() - previousRaw.getAltitude();

            double distance = distanceCalculator.calculateHaversineDistance(
                    previousRaw.getLatitude(), previousRaw.getLongitude(),
                    dto.getLatitude(), dto.getLongitude());

            totalDistanceHaversine = previousRaw.getTotalDistance() + distance;
        }

        RawTelemetry rawTelemetry = new RawTelemetry();
        rawTelemetry.setDrone(drone);
        rawTelemetry.setLatitude(dto.getLatitude());
        rawTelemetry.setLongitude(dto.getLongitude());
        rawTelemetry.setAltitude(dto.getAltitude());
        rawTelemetry.setSpeed(dto.getSpeed());
        rawTelemetry.setGpsAccuracy(dto.getGpsAccuracy());
        rawTelemetry.setLocalDateTime(LocalDateTime.now());
        rawTelemetry.setAltitudeChange(altitudeChange);
        rawTelemetry.setTotalDistance(totalDistance);
        rawTelemetry.setTotalDistanceHaversine(totalDistanceHaversine);

        return rawTelemetryRepository.save(rawTelemetry);
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

    @Transactional(readOnly = true)
    public List<Telemetry> getProcessedTelemetry(Long droneId) {
        return telemetryRepository.findTop50ByDroneIdOrderByLocalDateTimeDesc(droneId);
    }

}
