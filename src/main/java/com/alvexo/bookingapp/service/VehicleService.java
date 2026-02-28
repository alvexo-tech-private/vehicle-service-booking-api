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
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;

@Service
public class VehicleService {
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request, User admin) {
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("Only administrators can create vehicles");
        }
        
        if (request.getVin() != null && vehicleRepository.existsByVin(request.getVin())) {
            throw new BadRequestException("Vehicle with this VIN already exists");
        }
        
        Vehicle vehicle = Vehicle.builder()
                .make(request.getManufacturer())
                .model(request.getModelName())
                .year(request.getYear())
                .vin(request.getVin())
                .licensePlate(request.getLicensePlate())
                .vehicleType(request.getVehicleType())
                .color(request.getColor())
                .mileage(request.getMileage())
                .engineType(request.getEngineType())
                .transmissionType(request.getTransmissionType())
                .fuelType(request.getFuelType())
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
        vehicle.setVin(request.getVin());
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setColor(request.getColor());
        vehicle.setMileage(request.getMileage());
        vehicle.setEngineType(request.getEngineType());
        vehicle.setTransmissionType(request.getTransmissionType());
        vehicle.setFuelType(request.getFuelType());
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
                .vin(vehicle.getVin())
                .licensePlate(vehicle.getLicensePlate())
                .vehicleType(vehicle.getVehicleType())
                .color(vehicle.getColor())
                .mileage(vehicle.getMileage())
                .engineType(vehicle.getEngineType())
                .transmissionType(vehicle.getTransmissionType())
                .fuelType(vehicle.getFuelType())
                .imageUrl(vehicle.getImageUrl())
                .active(vehicle.getActive())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
