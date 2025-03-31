package com.example.DroneTelemetrySystem.repositories;

import com.example.DroneTelemetrySystem.models.RawTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawTelemetryRepository extends JpaRepository<RawTelemetry, Long> {
    List<RawTelemetry> findTop1ByDroneIdOrderByLocalDateTimeDesc(Long droneId);
    List<RawTelemetry> findByDroneId(Long droneId);

    List<RawTelemetry> findByDroneIdOrderByLocalDateTimeAsc(Long id);
}
