package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.DropAssignRequest;
import com.alvexo.bookingapp.dto.request.DropDeliverRequest;
import com.alvexo.bookingapp.dto.response.DropSummaryResponse;
import com.alvexo.bookingapp.dto.response.DropVehicleResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ServiceDeskDropService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Service Desk - Drop", description = "SERVICE_DESK_API_SPEC.md §4 — today's drops, no history.")
@RestController
@RequestMapping("/api/service-desk/drops")
@PreAuthorize("hasRole('MECHANIC')")
public class ServiceDeskDropController {

    private final ServiceDeskDropService dropService;
    private final UserRepository userRepository;

    public ServiceDeskDropController(ServiceDeskDropService dropService, UserRepository userRepository) {
        this.dropService = dropService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get today's drops")
    @GetMapping
    public ResponseEntity<MyApiResponse<DropSummaryResponse>> getDrops(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(dropService.getDrops(resolveUser(authentication), date)));
    }

    @Operation(summary = "Assign or change the drop mechanic")
    @PutMapping("/{bookingId}/assign")
    public ResponseEntity<MyApiResponse<DropVehicleResponse>> assign(
            @PathVariable String bookingId,
            @Valid @RequestBody DropAssignRequest request,
            Authentication authentication) {
        DropVehicleResponse response =
                dropService.assign(resolveUser(authentication), bookingId, request.getMechanicId());
        return ResponseEntity.ok(MyApiResponse.success("Mechanic assigned", response));
    }

    @Operation(summary = "Mark a vehicle delivered")
    @PutMapping("/{bookingId}/deliver")
    public ResponseEntity<MyApiResponse<DropVehicleResponse>> deliver(
            @PathVariable String bookingId,
            @Valid @RequestBody DropDeliverRequest request,
            Authentication authentication) {
        DropVehicleResponse response = dropService.deliver(
                resolveUser(authentication), bookingId, request.getDeliveredBy(), request.getDeliveredOn());
        return ResponseEntity.ok(MyApiResponse.success("Vehicle marked delivered", response));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
