package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.VehicleRequest;
import com.alvexo.bookingapp.dto.response.VehicleResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.VehicleRepository;

@Service
public class VehicleService {
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request, User admin) {
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("Only administrators can create vehicles");
        }
        
        
        Vehicle vehicle = Vehicle.builder()
                .make(request.getManufacturer())
                .model(request.getModelName())
                .year(request.getYear())
                .vehicleType(request.getVehicleType())
                .imageUrl(request.getImageUrl())
                .createdBy(admin)
                .build();
        
        vehicle = vehicleRepository.save(vehicle);
        return convertToResponse(vehicle);
    }
    
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request, User admin) {
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("Only administrators can update vehicles");
        }
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        
        vehicle.setMake(request.getManufacturer());
        vehicle.setModel(request.getModelName());
        vehicle.setYear(request.getYear());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setImageUrl(request.getImageUrl());
        
        vehicle = vehicleRepository.save(vehicle);
        return convertToResponse(vehicle);
    }
    
    @Transactional
    public void deleteVehicle(Long id, User admin) {
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("Only administrators can delete vehicles");
        }
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }
    
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return convertToResponse(vehicle);
    }
    
    public Page<VehicleResponse> getAllActiveVehicles(Pageable pageable) {
        return vehicleRepository.findByActiveTrue(pageable)
                .map(this::convertToResponse);
    }
    
    private VehicleResponse convertToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .vehicleType(vehicle.getVehicleType())
                .engineType(vehicle.getEngineType())
                .imageUrl(vehicle.getImageUrl())
                .active(vehicle.getActive())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
