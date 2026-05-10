package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.model.FuelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

import java.util.List;

@Tag(name = "Vehicles", description = "Vehicle catalogue management. Creating and updating vehicles requires ADMINISTRATOR role.")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserRepository userRepository;
    
    
    
    @Operation(summary = "Create a vehicle", description = "Adds a new vehicle make/model to the catalogue.")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
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

    // ── Catalogue filters (authenticated users) ──────────────────────────────

    @Operation(
        summary = "Get all fuel types",
        description = "Returns the list of all supported fuel types (PETROL, DIESEL, ELECTRIC). Step 1 of the vehicle selection flow."
    )
    @GetMapping("/fuel-types")
    public ResponseEntity<MyApiResponse<List<FuelType>>> getFuelTypes() {
        List<FuelType> fuelTypes = vehicleService.getAllFuelTypes();
        return ResponseEntity.ok(MyApiResponse.success(fuelTypes));
    }

    @Operation(
        summary = "Get manufacturers by fuel type",
        description = "Returns distinct manufacturers that have active vehicles for the given fuel type. Step 2 of the vehicle selection flow."
    )
    @GetMapping("/manufacturers")
    public ResponseEntity<MyApiResponse<List<String>>> getManufacturers(
            @RequestParam FuelType fuelType) {
        log.info("Fetching manufacturers for fuelType={}", fuelType);
        List<String> manufacturers = vehicleService.getManufacturersByFuelType(fuelType);
        return ResponseEntity.ok(MyApiResponse.success(manufacturers));
    }

    @Operation(
        summary = "Get vehicles by fuel type and manufacturer",
        description = "Returns paginated vehicles filtered by fuel type and optionally by manufacturer. Step 3 of the vehicle selection flow."
    )
    @GetMapping
    public ResponseEntity<MyApiResponse<Page<VehicleResponse>>> getVehicles(
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) String manufacturer,
            Pageable pageable) {
        log.info("Fetching vehicles with fuelType={}, manufacturer={}", fuelType, manufacturer);
        Page<VehicleResponse> vehicles = vehicleService.getVehiclesByFilter(fuelType, manufacturer, pageable);
        return ResponseEntity.ok(MyApiResponse.success(vehicles));
    }
    
    @Operation(summary = "Get vehicle by ID", description = "Returns details of a single vehicle by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<VehicleResponse>> getVehicleById(@PathVariable Long id) {
        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(MyApiResponse.success(vehicle));
    }

	
}
