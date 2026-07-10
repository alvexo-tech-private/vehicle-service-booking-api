package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.MechanicConfigurationSettingsRequest;
import com.alvexo.bookingapp.dto.request.MechanicMasterEntryRequest;
import com.alvexo.bookingapp.dto.response.MechanicConfigurationSettingsResponse;
import com.alvexo.bookingapp.dto.response.MechanicMasterEntryResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicConfigurationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mechanic Configuration Settings",
     description = "Job-card-number format, reschedule policy, pickup/drop, mechanic master, and service reminder intervals.")
@RestController
@RequestMapping("/api/mechanic-settings/configuration")
public class MechanicConfigurationSettingsController {

    private final MechanicConfigurationSettingsService configService;
    private final UserRepository userRepository;

    public MechanicConfigurationSettingsController(MechanicConfigurationSettingsService configService,
                                                     UserRepository userRepository) {
        this.configService = configService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Save configuration settings (create or update)")
    @PutMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicConfigurationSettingsResponse>> saveConfiguration(
            @Valid @RequestBody MechanicConfigurationSettingsRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success("Configuration settings saved successfully",
                configService.saveConfiguration(mechanic, request)));
    }

    @Operation(summary = "Get my configuration settings")
    @GetMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicConfigurationSettingsResponse>> getConfiguration(
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(configService.getConfiguration(mechanic)));
    }

    // ── Mechanic Master ─────────────────────────────────────────────────────

    @Operation(summary = "Add a mechanic master entry")
    @PostMapping("/mechanics")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicMasterEntryResponse>> addMechanicMasterEntry(
            @Valid @RequestBody MechanicMasterEntryRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Mechanic added successfully",
                        configService.addMechanicMasterEntry(mechanic, request)));
    }

    @Operation(summary = "Update a mechanic master entry")
    @PutMapping("/mechanics/{entryId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicMasterEntryResponse>> updateMechanicMasterEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody MechanicMasterEntryRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success("Mechanic updated successfully",
                configService.updateMechanicMasterEntry(mechanic, entryId, request)));
    }

    @Operation(summary = "Remove a mechanic master entry")
    @DeleteMapping("/mechanics/{entryId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteMechanicMasterEntry(
            @PathVariable Long entryId,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        configService.deleteMechanicMasterEntry(mechanic, entryId);
        return ResponseEntity.ok(MyApiResponse.success("Mechanic removed successfully", null));
    }

    @Operation(summary = "List mechanic master entries for a workshop")
    @GetMapping("/{mechanicId}/mechanics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicMasterEntryResponse>>> getMechanicMasterEntries(
            @PathVariable Long mechanicId) {
        return ResponseEntity.ok(MyApiResponse.success(configService.getMechanicMasterEntries(mechanicId)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
