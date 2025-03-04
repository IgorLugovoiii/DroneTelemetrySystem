package com.example.DroneTelemetrySystem.models;

import com.example.DroneTelemetrySystem.models.enums.ProcessingType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "raw_telemetry")
public class RawTelemetry {
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

    @Column(name = "gps_accuracy")
    private double gpsAccuracy;

    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "total_distance")
    private double totalDistance;

    @Column(name = "total_distance_haversine")
    private double totalDistanceHaversine;

    @Column(name = "processing_type")
    @Enumerated(EnumType.STRING)
    private ProcessingType processingType;

    @ManyToOne
    @JoinColumn(name = "drone_id", nullable = false)
    @JsonBackReference
    private Drone drone;
}
