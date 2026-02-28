package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserVehicle;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.UserVehicleService;

import java.util.List;

@RestController
@RequestMapping("/api/user-vehicles")
public class UserVehicleController {
    
    @Autowired
    private UserVehicleService userVehicleService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/map")
    public ResponseEntity<ApiResponse<UserVehicle>> mapVehicle(
            @RequestParam Long vehicleId,
            @RequestParam(required = false) Boolean isPrimary,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserVehicle userVehicle = userVehicleService.mapVehicleToUser(user, vehicleId, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle mapped successfully", userVehicle));
    }
    
    @GetMapping("/my-vehicles")
    public ResponseEntity<ApiResponse<List<UserVehicle>>> getMyVehicles(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<UserVehicle> vehicles = userVehicleService.getUserVehicles(user);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }
    
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<ApiResponse<Void>> removeVehicleMapping(
            @PathVariable Long mappingId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        userVehicleService.removeVehicleMapping(mappingId, user);
        return ResponseEntity.ok(ApiResponse.success("Vehicle mapping removed", null));
    }
}
