package com.alvexo.bookingapp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {
    
    @Autowired
    private MechanicService mechanicService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/availability")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> addAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.addAvailability(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Availability added successfully", response));
    }
    
    @GetMapping("/availability")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getMyAvailability(
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<AvailabilityResponse> availability = mechanicService.getMechanicAvailability(mechanic);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }
    
    @PutMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.updateAvailability(id, request, mechanic);
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", response));
    }
    
    @DeleteMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<ApiResponse<Void>> deleteAvailability(
            @PathVariable Long id,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        mechanicService.deleteAvailability(id, mechanic);
        return ResponseEntity.ok(ApiResponse.success("Availability deleted successfully", null));
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findNearbyMechanics(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        List<UserResponse> mechanics = mechanicService.findNearbyMechanics(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(mechanics));
    }
}
