package com.example.DroneTelemetrySystem.services;

import com.example.DroneTelemetrySystem.models.Drone;
import com.example.DroneTelemetrySystem.repositories.DroneRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DroneService {
    private final DroneRepository droneRepository;

    @Autowired
    public DroneService(DroneRepository droneRepository){
        this.droneRepository = droneRepository;
    }

    @Transactional(readOnly = true)
    public List<Drone> findAll(){
        return droneRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Drone findById(Long id){
        return droneRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Transactional
    public Drone createDrone(Drone drone){
        return droneRepository.save(drone);
    }

    @Transactional
    public Drone updateDrone(Drone updatedDrone, Long id){
        Drone existingDrone = droneRepository.findById(id).orElseThrow(EntityNotFoundException::new);

        existingDrone.setName(updatedDrone.getName());
        existingDrone.setTelemetryList(updatedDrone.getTelemetryList());

        return droneRepository.save(existingDrone);
    }

    @Transactional
    public void deleteDroneById(Long id){
        droneRepository.deleteById(id);
    }
}
