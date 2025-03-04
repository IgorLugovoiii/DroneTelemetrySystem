package com.example.DroneTelemetrySystem.controllers;

import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.services.DroneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drones")
public class DroneController {
    private final DroneService droneService;

    @Autowired
    public DroneController(DroneService droneService) {
        this.droneService = droneService;
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
}
