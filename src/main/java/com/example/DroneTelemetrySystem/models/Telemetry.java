package com.example.DroneTelemetrySystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "telemetry")
public class Telemetry {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "latitude")
    private double latitude;
    @Column(name = "longitude")
    private double longitude;
    @Column(name = "altitude")
    private double altitude;
    @Column(name = "altitude_change")
    private double altitudeChange;
    @Column(name = "speed")
    private double speed;
    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;
    @Column(name = "total_distance")
    private double totalDistance;
    @Column(name = "total_distance_haversine")
    private double totalDistanceHaversine;
    @ManyToOne
    @JoinColumn(name = "drone_id", nullable = false)
    private Drone drone;
}
