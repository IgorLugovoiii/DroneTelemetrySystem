package com.example.DroneTelemetrySystem.repositories;

import com.example.DroneTelemetrySystem.models.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    List<Telemetry> findTop1ByDroneIdOrderByLocalDateTimeDesc(Long droneId);

    List<Telemetry> findTop50ByDroneIdOrderByLocalDateTimeDesc(Long droneId);

    List<Telemetry> findByDroneIdOrderByLocalDateTimeAsc(Long id);

    @Modifying
    @Query("DELETE FROM Telemetry t WHERE t.drone.id = :droneId")
    void deleteByDroneId(@Param("droneId") Long droneId);
}
