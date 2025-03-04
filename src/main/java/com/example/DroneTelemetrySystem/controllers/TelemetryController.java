package com.example.DroneTelemetrySystem.controllers;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.services.TelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    private final TelemetryService telemetryService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
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

    @GetMapping("/last/{droneId}")
    public ResponseEntity<List<Telemetry>> getLastTelemetry(@PathVariable Long droneId) {
        List<Telemetry> telemetryList = telemetryService.getLastTelemetry(droneId);
        return ResponseEntity.ok(telemetryList);
    }

    @GetMapping("/history/{droneId}")
    public ResponseEntity<List<Telemetry>> getTelemetryHistory(
            @PathVariable Long droneId,
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        List<Telemetry> telemetryList = telemetryService.getTelemetryHistory(droneId, startDate, endDate);
        return ResponseEntity.ok(telemetryList);
    }
}
