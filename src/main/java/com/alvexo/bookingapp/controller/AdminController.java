package com.alvexo.bookingapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.AdministratorRegisterRequest;
import com.alvexo.bookingapp.dto.request.SalesRepresentativeRegisterRequest;
import com.alvexo.bookingapp.dto.request.VehicleRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.TokenResponse;
import com.alvexo.bookingapp.dto.response.VehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.AuthService;
import com.alvexo.bookingapp.service.VehicleService;

@Tag(name = "Admin — User Management", description = "Administrator-only endpoints for creating SALES_REPRESENTATIVE and ADMINISTRATOR accounts, and managing vehicles. Requires JWT with ADMINISTRATOR role.")
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
    @Operation(summary = "Create a sales representative", description = "Creates a SALES_REPRESENTATIVE account. A unique referral code is auto-generated. Requires ADMINISTRATOR role.")
    @PostMapping("/users/sales-representative")
    public ResponseEntity<MyApiResponse<TokenResponse>> createSalesRepresentative(
            @Valid @RequestBody SalesRepresentativeRegisterRequest request) {
        TokenResponse response = authService.registerSalesRepresentative(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Sales representative created successfully", response));
    }

    /**
     * POST /api/admin/users/administrator
     *
     * Creates a new Administrator account.
     * Only an existing authenticated ADMINISTRATOR can create another administrator.
     */
    @Operation(summary = "Create an administrator", description = "Creates a new ADMINISTRATOR account. Only an existing authenticated ADMINISTRATOR can call this. Requires ADMINISTRATOR role.")
    @PostMapping("/users/administrator")
    public ResponseEntity<MyApiResponse<TokenResponse>> createAdministrator(
            @Valid @RequestBody AdministratorRegisterRequest request) {
        TokenResponse response = authService.registerAdministrator(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Administrator created successfully", response));
    }

    // -------------------------------------------------------------------------
    // Vehicle management
    // -------------------------------------------------------------------------

    @Operation(summary = "Create a vehicle", description = "Adds a new vehicle to the system catalogue. Requires ADMINISTRATOR role.")
    @PostMapping("/vehicles")
    public ResponseEntity<MyApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VehicleResponse response = vehicleService.createVehicle(request, admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Vehicle created successfully", response));
    }
    
    @Operation(summary = "Update a vehicle", description = "Updates an existing vehicle by ID. Requires ADMINISTRATOR role.")
    @PutMapping("/vehicles/{id}")
    public ResponseEntity<MyApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VehicleResponse response = vehicleService.updateVehicle(id, request, admin);
        return ResponseEntity.ok(MyApiResponse.success("Vehicle updated successfully", response));
    }
    
    @Operation(summary = "Delete a vehicle", description = "Removes a vehicle from the catalogue by ID. Requires ADMINISTRATOR role.")
    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<MyApiResponse<Void>> deleteVehicle(
            @PathVariable Long id,
            Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        vehicleService.deleteVehicle(id, admin);
        return ResponseEntity.ok(MyApiResponse.success("Vehicle deleted successfully", null));
    }
}
