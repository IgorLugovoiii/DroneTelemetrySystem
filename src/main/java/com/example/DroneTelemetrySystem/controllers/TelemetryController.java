package com.example.DroneTelemetrySystem.controllers;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.models.RawTelemetry;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.repositories.RawTelemetryRepository;
import com.example.DroneTelemetrySystem.services.TelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "http://localhost:4200")
public class TelemetryController {
    private final TelemetryService telemetryService;
    private final RawTelemetryRepository rawTelemetryRepository;

    @Autowired
    public TelemetryController(TelemetryService telemetryService, RawTelemetryRepository rawTelemetryRepository) {
        this.telemetryService = telemetryService;
        this.rawTelemetryRepository = rawTelemetryRepository;
    }

    @PostMapping("/saveRawTelemetry")
    public ResponseEntity<RawTelemetry> saveRawTelemetry(@RequestBody TelemetryDto telemetryDto) {
        return new ResponseEntity<>(telemetryService.saveRawTelemetry(telemetryDto), HttpStatus.CREATED);
    }

    @PostMapping("/kalman")
    public ResponseEntity<Telemetry> processWithKalman(@RequestBody TelemetryDto telemetryDto) {
        Telemetry telemetry = telemetryService.processWithKalmanFilter(telemetryDto);
        return new ResponseEntity<>(telemetry, HttpStatus.CREATED);
    }

    @PostMapping("/haversine")
    public ResponseEntity<Telemetry> processWithHaversine(@RequestBody TelemetryDto telemetryDto) {
        Telemetry telemetry = telemetryService.processWithHaversine(telemetryDto);
        return new ResponseEntity<>(telemetry, HttpStatus.CREATED);
    }

    @PostMapping("/haversine-kalman")
    public ResponseEntity<Telemetry> processWithHaversineKalman(@RequestBody TelemetryDto telemetryDto) {
        Telemetry telemetry = telemetryService.processWithKalmanAndHaversine(telemetryDto);
        return new ResponseEntity<>(telemetry, HttpStatus.CREATED);
    }

    @GetMapping("/raw/{droneId}")
    public ResponseEntity<List<RawTelemetry>> getRawTelemetry(@PathVariable Long droneId) {
        return new ResponseEntity<>(rawTelemetryRepository.findByDroneId(droneId), HttpStatus.OK);
    }

    @GetMapping("/processed/{droneId}")
    public ResponseEntity<List<Telemetry>> getProcessedTelemetry(@PathVariable Long droneId) {
        return new ResponseEntity<>(telemetryService.getProcessedTelemetry(droneId), HttpStatus.OK);
    }
    @DeleteMapping("/clear/{droneId}")
    public ResponseEntity<Void> clearProcessedTelemetry(@PathVariable Long droneId) {
        telemetryService.deleteByDroneId(droneId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/processAll/kalman/{droneId}")
    public ResponseEntity<List<Telemetry>> processAllWithKalman(@PathVariable Long droneId) {
        return new ResponseEntity<>(telemetryService.processAllWithKalman(droneId), HttpStatus.OK);
    }

    @PostMapping("/processAll/haversine/{droneId}")
    public ResponseEntity<List<Telemetry>> processAllWithHaversine(@PathVariable Long droneId) {
        return new ResponseEntity<>(telemetryService.processAllWithHaversine(droneId), HttpStatus.OK);
    }

    @PostMapping("/processAll/kalman-haversine/{droneId}")
    public ResponseEntity<List<Telemetry>> processAllWithKalmanAndHaversine(@PathVariable Long droneId) {
        return new ResponseEntity<>(telemetryService.processAllWithKalmanAndHaversine(droneId), HttpStatus.OK);
    }
}
