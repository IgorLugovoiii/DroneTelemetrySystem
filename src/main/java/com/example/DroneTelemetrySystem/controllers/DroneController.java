package com.example.DroneTelemetrySystem.controllers;

import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.models.RawTelemetry;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.repositories.DroneRepository;
import com.example.DroneTelemetrySystem.repositories.RawTelemetryRepository;
import com.example.DroneTelemetrySystem.repositories.TelemetryRepository;
import com.example.DroneTelemetrySystem.services.DroneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
@CrossOrigin(origins = "http://localhost:4200")
public class DroneController {
    private final DroneService droneService;
    private final DroneRepository droneRepository;
    private final TelemetryRepository telemetryRepository;
    private final RawTelemetryRepository rawTelemetryRepository;

    @Autowired
    public DroneController(DroneService droneService, DroneRepository droneRepository, TelemetryRepository telemetryRepository,
                           RawTelemetryRepository rawTelemetryRepository) {
        this.droneService = droneService;
        this.droneRepository = droneRepository;
        this.telemetryRepository = telemetryRepository;
        this.rawTelemetryRepository = rawTelemetryRepository;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Drone>> findAllDrones() {
        return new ResponseEntity<>(droneService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Drone> findDroneById(@PathVariable Long id) {
        return new ResponseEntity<>(droneService.findById(id), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Drone> createDrone(@RequestBody Drone drone) {
        return new ResponseEntity<>(droneService.createDrone(drone), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Drone> updateDrone(@RequestBody Drone drone, @PathVariable Long id) {
        return new ResponseEntity<>(droneService.updateDrone(drone, id), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDroneByID(@PathVariable Long id) {
        droneService.deleteDroneById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/by-name")
    public ResponseEntity<Drone> getDroneByName(@RequestParam String name) {
        Drone drone = droneRepository.findByName(name);
        if (drone != null) {
            return new ResponseEntity<>(drone, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<List<RawTelemetry>> getDronePath(@PathVariable Long id) {
        List<RawTelemetry> path = rawTelemetryRepository.findByDroneIdOrderByLocalDateTimeAsc(id);
        return new ResponseEntity<>(path, HttpStatus.OK);
    }
}
