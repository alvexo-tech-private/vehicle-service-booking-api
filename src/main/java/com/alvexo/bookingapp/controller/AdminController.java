package com.alvexo.bookingapp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.AdministratorRegisterRequest;
import com.alvexo.bookingapp.dto.request.SalesRepresentativeRegisterRequest;
import com.alvexo.bookingapp.dto.request.VehicleRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.dto.response.VehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.AuthService;
import com.alvexo.bookingapp.service.VehicleService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {
    
    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // User management — admin creates privileged users
    // -------------------------------------------------------------------------

    /**
     * POST /api/admin/users/sales-representative
     *
     * Creates a new Sales Representative account.
     * A unique referral code is auto-generated and included in the response token.
     * Only an authenticated ADMINISTRATOR can call this endpoint.
     */
    @PostMapping("/users/sales-representative")
    public ResponseEntity<ApiResponse<TokenResponse>> createSalesRepresentative(
            @Valid @RequestBody SalesRepresentativeRegisterRequest request) {
        TokenResponse response = authService.registerSalesRepresentative(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sales representative created successfully", response));
    }

    /**
     * POST /api/admin/users/administrator
     *
     * Creates a new Administrator account.
     * Only an existing authenticated ADMINISTRATOR can create another administrator.
     */
    @PostMapping("/users/administrator")
    public ResponseEntity<ApiResponse<TokenResponse>> createAdministrator(
            @Valid @RequestBody AdministratorRegisterRequest request) {
        TokenResponse response = authService.registerAdministrator(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Administrator created successfully", response));
    }

    // -------------------------------------------------------------------------
    // Vehicle management
    // -------------------------------------------------------------------------

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
