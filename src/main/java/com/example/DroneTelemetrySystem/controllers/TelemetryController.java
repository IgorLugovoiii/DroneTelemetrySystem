package com.example.DroneTelemetrySystem.controllers;

import com.example.DroneTelemetrySystem.dtos.TelemetryDto;
import com.example.DroneTelemetrySystem.models.Telemetry;
import com.example.DroneTelemetrySystem.services.TelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    private final TelemetryService telemetryService;

    @Autowired
    public TelemetryController(TelemetryService telemetryService){
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public ResponseEntity<List<Telemetry>> getTelemetry() {
        List<Telemetry> telemetryList = telemetryService.getLastTelemetry();
        return ResponseEntity.ok(telemetryList);
    }

//    @PostMapping
//    public ResponseEntity<Telemetry> receiveTelemetry(@RequestBody TelemetryDto dto) {
//        Telemetry savedData = telemetryService.processTelemetry(dto);
//        return ResponseEntity.ok(savedData);
//    }
}
