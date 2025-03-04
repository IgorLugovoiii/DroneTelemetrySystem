package com.example.DroneTelemetrySystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "drone")
@Data
public class Drone {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @OneToMany(mappedBy = "drone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Telemetry> telemetryList;
}
