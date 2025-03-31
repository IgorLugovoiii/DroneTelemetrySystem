package com.example.DroneTelemetrySystem.services;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.filters.DistanceCalculator;
import com.example.DroneTelemetrySystem.filters.KalmanFilter;
import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.models.RawTelemetry;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.models.enums.ProcessingType;
import com.example.DroneTelemetrySystem.repositories.DroneRepository;
import com.example.DroneTelemetrySystem.repositories.RawTelemetryRepository;
import com.example.DroneTelemetrySystem.repositories.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // Отримуємо останню оброблену телеметрію для цього дрона
        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());

        // Ініціалізуємо фільтри Калмана
        KalmanFilter latFilter = new KalmanFilter(lastTelemetry.isEmpty() ? dto.getLatitude() : lastTelemetry.get(0).getLatitude());
        KalmanFilter lonFilter = new KalmanFilter(lastTelemetry.isEmpty() ? dto.getLongitude() : lastTelemetry.get(0).getLongitude());

        // Фільтруємо координати
        double filteredLat = latFilter.update(dto.getLatitude(), dto.getSpeed(), dto.getGpsAccuracy());
        double filteredLon = lonFilter.update(dto.getLongitude(), dto.getSpeed(), dto.getGpsAccuracy());

        // Розраховуємо відстань та зміну висоти
        double totalDistance = 0.0;
        double totalDistanceHaversine = 0.0;
        double altitudeChange = 0.0;

        if (!lastTelemetry.isEmpty()) {
            Telemetry prev = lastTelemetry.getFirst();
            // Відстань між попередньою обробленою точкою та поточною фільтрованою
            totalDistanceHaversine = distanceCalculator.calculateHaversineDistance(
                    prev.getLatitude(), prev.getLongitude(),
                    filteredLat, filteredLon);

            // Загальна відстань
            totalDistance = prev.getTotalDistance() + totalDistanceHaversine;
            altitudeChange = dto.getAltitude() - prev.getAltitude();
        }

        // Створюємо нову оброблену телеметрію
        Telemetry telemetry = new Telemetry();
        telemetry.setDrone(drone);
        telemetry.setLatitude(filteredLat);
        telemetry.setLongitude(filteredLon);
        telemetry.setAltitude(dto.getAltitude());
        telemetry.setSpeed(dto.getSpeed());
        telemetry.setLocalDateTime(LocalDateTime.now());
        telemetry.setAltitudeChange(altitudeChange);
        telemetry.setTotalDistance(totalDistance);
        telemetry.setTotalDistanceHaversine(totalDistanceHaversine);
        telemetry.setProcessingType(ProcessingType.KALMAN);

        return telemetryRepository.save(telemetry);
    }

    @Transactional
    public Telemetry processWithHaversine(TelemetryDto dto) {
        Drone drone = droneRepository.findById(dto.getDroneId())
                .orElseThrow(() -> new RuntimeException("Drone not found"));

        // Отримуємо останню оброблену телеметрію
        List<Telemetry> lastTelemetry = telemetryRepository.findTop1ByDroneIdOrderByLocalDateTimeDesc(dto.getDroneId());

        double totalDistance = 0.0;
        double totalDistanceHaversine = 0.0;
        double altitudeChange = 0.0;

        if (!lastTelemetry.isEmpty()) {
            Telemetry prev = lastTelemetry.get(0);
            // Розраховуємо відстань за формулою Гаверсина
            totalDistanceHaversine = distanceCalculator.calculateHaversineDistance(
                    prev.getLatitude(), prev.getLongitude(),
                    dto.getLatitude(), dto.getLongitude());

            totalDistance = prev.getTotalDistance() + totalDistanceHaversine;
            altitudeChange = dto.getAltitude() - prev.getAltitude();
        }

        // Створюємо нову телеметрію
        Telemetry telemetry = new Telemetry();
        telemetry.setDrone(drone);
        telemetry.setLatitude(dto.getLatitude());  // Беремо оригінальні координати
        telemetry.setLongitude(dto.getLongitude());
        telemetry.setAltitude(dto.getAltitude());
        telemetry.setSpeed(dto.getSpeed());
        telemetry.setLocalDateTime(LocalDateTime.now());
        telemetry.setAltitudeChange(altitudeChange);
        telemetry.setTotalDistance(totalDistance);
        telemetry.setTotalDistanceHaversine(totalDistanceHaversine);
        telemetry.setProcessingType(ProcessingType.HAVERSINE);

        return telemetryRepository.save(telemetry);
    }

    @Transactional
    public Telemetry processWithKalmanAndHaversine(TelemetryDto dto) {
        // Комбінуємо підходи Kalman та Haversine
        Telemetry kalmanResult = processWithKalmanFilter(dto);
        TelemetryDto haversineDto = new TelemetryDto(
                dto.getId(),
                kalmanResult.getLatitude(),  // Беремо відфільтровані координати
                kalmanResult.getLongitude(),
                dto.getAltitude(),
                dto.getSpeed(),
                dto.getGpsAccuracy(),
                dto.getDroneId(),
                dto.getAltitudeChange()
        );

        return processWithHaversine(haversineDto);
    }

    @Transactional(readOnly = true)
    public List<RawTelemetry> getAllRawTelemetryForDrone(Long droneId) {
        return rawTelemetryRepository.findByDroneIdOrderByLocalDateTimeAsc(droneId);
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getProcessedTelemetry(Long droneId) {
        return telemetryRepository.findTop50ByDroneIdOrderByLocalDateTimeDesc(droneId);
    }

    @Transactional
    public void deleteByDroneId(Long droneId) {
        telemetryRepository.deleteByDroneId(droneId);
    }

    @Transactional
    public List<Telemetry> processAllWithKalman(Long droneId) {
        telemetryRepository.deleteByDroneId(droneId);
        List<RawTelemetry> rawList = rawTelemetryRepository.findByDroneIdOrderByLocalDateTimeAsc(droneId);
        List<Telemetry> result = new ArrayList<>();

        if (rawList.isEmpty()) return result;

        // Ініціалізація фільтрів з першою точкою
        KalmanFilter latFilter = new KalmanFilter(rawList.get(0).getLatitude());
        KalmanFilter lonFilter = new KalmanFilter(rawList.get(0).getLongitude());

        Telemetry prev = null;

        for (RawTelemetry raw : rawList) {
            // Фільтрація координат
            double filteredLat = latFilter.update(raw.getLatitude(), raw.getSpeed(), raw.getGpsAccuracy());
            double filteredLon = lonFilter.update(raw.getLongitude(), raw.getSpeed(), raw.getGpsAccuracy());

            // Розрахунок відстаней
            double distance = 0;
            double altitudeChange = 0;

            if (prev != null) {
                distance = distanceCalculator.calculateHaversineDistance(
                        prev.getLatitude(), prev.getLongitude(),
                        filteredLat, filteredLon
                );
                altitudeChange = raw.getAltitude() - prev.getAltitude();
            }

            Telemetry t = new Telemetry();
            t.setDrone(raw.getDrone());
            t.setLatitude(filteredLat);
            t.setLongitude(filteredLon);
            t.setAltitude(raw.getAltitude());
            t.setSpeed(raw.getSpeed());
            t.setLocalDateTime(raw.getLocalDateTime());
            t.setAltitudeChange(altitudeChange);
            t.setTotalDistance(prev != null ? prev.getTotalDistance() + distance : 0);
            t.setTotalDistanceHaversine(distance);
            t.setProcessingType(ProcessingType.KALMAN);

            result.add(telemetryRepository.save(t));
            prev = t;
        }

        return result;
    }

    @Transactional
    public List<Telemetry> processAllWithHaversine(Long droneId) {
        telemetryRepository.deleteByDroneId(droneId);

        List<RawTelemetry> rawTelemetryList = rawTelemetryRepository.findByDroneIdOrderByLocalDateTimeAsc(droneId);
        List<Telemetry> processedTelemetry = new ArrayList<>();

        Telemetry previous = null;

        for (RawTelemetry raw : rawTelemetryList) {
            double totalDistance = 0.0;
            double totalDistanceHaversine = 0.0;
            double altitudeChange = 0.0;

            if (previous != null) {
                totalDistanceHaversine = distanceCalculator.calculateHaversineDistance(
                        previous.getLatitude(), previous.getLongitude(),
                        raw.getLatitude(), raw.getLongitude());

                totalDistance = previous.getTotalDistance() + totalDistanceHaversine;
                altitudeChange = raw.getAltitude() - previous.getAltitude();
            }

            Telemetry telemetry = new Telemetry();
            telemetry.setDrone(raw.getDrone());
            telemetry.setLatitude(raw.getLatitude());
            telemetry.setLongitude(raw.getLongitude());
            telemetry.setAltitude(raw.getAltitude());
            telemetry.setSpeed(raw.getSpeed());
            telemetry.setLocalDateTime(raw.getLocalDateTime());
            telemetry.setAltitudeChange(altitudeChange);
            telemetry.setTotalDistance(totalDistance);
            telemetry.setTotalDistanceHaversine(totalDistanceHaversine);
            telemetry.setProcessingType(ProcessingType.HAVERSINE);

            processedTelemetry.add(telemetryRepository.save(telemetry));
            previous = telemetry;
        }

        return processedTelemetry;
    }

    @Transactional
    public List<Telemetry> processAllWithKalmanAndHaversine(Long droneId) {
        List<Telemetry> kalmanResults = processAllWithKalman(droneId);

        telemetryRepository.deleteByDroneId(droneId);

        List<Telemetry> finalResults = new ArrayList<>();
        Telemetry previous = null;

        for (Telemetry kalman : kalmanResults) {
            double totalDistance = 0.0;
            double totalDistanceHaversine = 0.0;
            double altitudeChange = 0.0;

            if (previous != null) {
                totalDistanceHaversine = distanceCalculator.calculateHaversineDistance(
                        previous.getLatitude(), previous.getLongitude(),
                        kalman.getLatitude(), kalman.getLongitude());

                totalDistance = previous.getTotalDistance() + totalDistanceHaversine;
                altitudeChange = kalman.getAltitude() - previous.getAltitude();
            }

            Telemetry telemetry = new Telemetry();
            telemetry.setDrone(kalman.getDrone());
            telemetry.setLatitude(kalman.getLatitude());
            telemetry.setLongitude(kalman.getLongitude());
            telemetry.setAltitude(kalman.getAltitude());
            telemetry.setSpeed(kalman.getSpeed());
            telemetry.setLocalDateTime(kalman.getLocalDateTime());
            telemetry.setAltitudeChange(altitudeChange);
            telemetry.setTotalDistance(totalDistance);
            telemetry.setTotalDistanceHaversine(totalDistanceHaversine);
            telemetry.setProcessingType(ProcessingType.KALMAN_AND_HAVERSINE);

            finalResults.add(telemetryRepository.save(telemetry));
            previous = telemetry;
        }

        return finalResults;
    }
}
