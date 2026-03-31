package com.alvexo.bookingapp.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Mechanics", description = "Mechanic availability management and nearby mechanic search. Requires JWT token.")
@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {
    
    @Autowired
    private MechanicService mechanicService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/availability")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<AvailabilityResponse>> addAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.addAvailability(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Availability added successfully", response));
    }
    
    @GetMapping("/availability")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<AvailabilityResponse>>> getMyAvailability(
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<AvailabilityResponse> availability = mechanicService.getMechanicAvailability(mechanic);
        return ResponseEntity.ok(MyApiResponse.success(availability));
    }
    
    @PutMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<AvailabilityResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.updateAvailability(id, request, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Availability updated successfully", response));
    }
    
    @DeleteMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteAvailability(
            @PathVariable Long id,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        mechanicService.deleteAvailability(id, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Availability deleted successfully", null));
    }
    
    @Operation(summary = "Find nearby mechanics", description = "Returns active mechanics within a given radius (km) of provided coordinates.")

    @GetMapping("/nearby")
    public ResponseEntity<MyApiResponse<List<UserResponse>>> findNearbyMechanics(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        List<UserResponse> mechanics = mechanicService.findNearbyMechanics(latitude, longitude, radiusKm);
        return ResponseEntity.ok(MyApiResponse.success(mechanics));
    }
}
