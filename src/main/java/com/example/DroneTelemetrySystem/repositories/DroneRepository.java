package com.example.DroneTelemetrySystem.repositories;

import com.example.DroneTelemetrySystem.models.Drone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {
    Drone findByName(String name);
}
