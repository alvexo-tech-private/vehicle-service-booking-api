package com.alvexo.bookingapp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.VehicleRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.VehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.VehicleService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VehicleResponse response = vehicleService.createVehicle(request, admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle created successfully", response));
    }
    
    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VehicleResponse response = vehicleService.updateVehicle(id, request, admin);
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", response));
    }
    
    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable Long id,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        vehicleService.deleteVehicle(id, admin);
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully", null));
    }
}
