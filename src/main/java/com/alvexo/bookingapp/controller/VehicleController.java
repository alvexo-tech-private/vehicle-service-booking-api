package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.VehicleRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.VehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.VehicleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Vehicles", description = "Vehicle catalogue management. Creating and updating vehicles requires ADMINISTRATOR role.")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class VehicleController {
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserRepository userRepository;
    
    
    
    @Operation(summary = "Create a vehicle", description = "Adds a new vehicle make/model to the catalogue.")
    @PostMapping
    public ResponseEntity<MyApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleRequest request,Authentication authentication) {
        log.info("Creating vehicle: {} {} {}", request.getManufacturer(), 
                 request.getModelName(), request.getYear());
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VehicleResponse response = vehicleService.createVehicle(request,user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Vehicle created successfully",response));
    }
    
    @Operation(summary = "Get all vehicles", description = "Returns paginated list of all vehicles in the catalogue.")
    @GetMapping
    public ResponseEntity<MyApiResponse<Page<VehicleResponse>>> getAllVehicles(Pageable pageable) {
        Page<VehicleResponse> vehicles = vehicleService.getAllActiveVehicles(pageable);
        return ResponseEntity.ok(MyApiResponse.success(vehicles));
    }
    
    @Operation(summary = "Get vehicle by ID", description = "Returns details of a single vehicle by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<VehicleResponse>> getVehicleById(@PathVariable Long id) {
        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(MyApiResponse.success(vehicle));
    }

	
}
