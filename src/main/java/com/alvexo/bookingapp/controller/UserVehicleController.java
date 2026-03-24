package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.UserVehicleRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.UserVehicleResponseDto;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.UserVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Vehicles", description = "Map and manage vehicles owned by the authenticated vehicle user. Requires JWT token.")
@RestController
@RequestMapping("/api/user-vehicles")
public class UserVehicleController {

    @Autowired
    private UserVehicleService userVehicleService;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Map a vehicle to user", description = "Associates an existing vehicle with the authenticated user account.")
    @PostMapping("/map")
    public ResponseEntity<MyApiResponse<UserVehicleResponseDto>> mapVehicle(@Valid @RequestBody UserVehicleRequest userVehicleRequest,
                                                                            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserVehicleResponseDto response = userVehicleService.mapVehicleToUser(user, userVehicleRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Vehicle mapped successfully", response));
    }

    @Operation(summary = "Get my vehicles", description = "Returns all vehicles mapped to the authenticated user.")
    @GetMapping("/my-vehicles")
    public ResponseEntity<MyApiResponse<List<UserVehicleResponseDto>>> getMyVehicles(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<UserVehicleResponseDto> vehicles = userVehicleService.getUserVehicles(user);
        return ResponseEntity.ok(MyApiResponse.success(vehicles));
    }

    @Operation(summary = "Remove vehicle mapping", description = "Removes the association between a vehicle and the user by mapping ID.")
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<MyApiResponse<Void>> removeVehicleMapping(
            @PathVariable Long mappingId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVehicleService.removeVehicleMapping(mappingId, user);
        return ResponseEntity.ok(MyApiResponse.success("Vehicle mapping removed", null));
    }
}