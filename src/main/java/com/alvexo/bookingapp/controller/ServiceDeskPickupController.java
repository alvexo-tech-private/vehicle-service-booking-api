package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.PickupAssignRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.PickupSummaryResponse;
import com.alvexo.bookingapp.dto.response.PickupVehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ServiceDeskPickupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Service Desk - Pickup", description = "SERVICE_DESK_API_SPEC.md §3 — today's pickups, no history.")
@RestController
@RequestMapping("/api/service-desk/pickups")
@PreAuthorize("hasRole('MECHANIC')")
public class ServiceDeskPickupController {

    private final ServiceDeskPickupService pickupService;
    private final UserRepository userRepository;

    public ServiceDeskPickupController(ServiceDeskPickupService pickupService, UserRepository userRepository) {
        this.pickupService = pickupService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get today's pickups")
    @GetMapping
    public ResponseEntity<MyApiResponse<PickupSummaryResponse>> getPickups(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(pickupService.getPickups(resolveUser(authentication), date)));
    }

    @Operation(summary = "Assign or change the pickup mechanic")
    @PutMapping("/{bookingId}/assign")
    public ResponseEntity<MyApiResponse<PickupVehicleResponse>> assign(
            @PathVariable String bookingId,
            @Valid @RequestBody PickupAssignRequest request,
            Authentication authentication) {
        PickupVehicleResponse response =
                pickupService.assign(resolveUser(authentication), bookingId, request.getMechanicId());
        return ResponseEntity.ok(MyApiResponse.success("Mechanic assigned", response));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
