package com.example.DroneTelemetrySystem.repositories;

import com.example.DroneTelemetrySystem.models.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    List<Telemetry> findTop1ByDroneIdOrderByLocalDateTimeDesc(Long droneId);
    List<Telemetry> findTop50ByDroneIdOrderByLocalDateTimeDesc(Long droneId);
    List<Telemetry> findByDroneIdAndLocalDateTimeBetweenOrderByLocalDateTimeAsc(Long droneId, LocalDateTime start, LocalDateTime end);

}
