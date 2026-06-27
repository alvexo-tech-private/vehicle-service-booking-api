package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.*;
import com.alvexo.bookingapp.dto.response.*;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mechanic Settings",
     description = "Manage mechanic booking settings: job card type, capacity, reporting times, "
                 + "advance payment, service catalogue, service slots, technician capacity, and daily overrides.")
@RestController
@RequestMapping("/api/mechanic-settings")
public class MechanicSettingsController {

    private final MechanicSettingsService settingsService;
    private final UserRepository userRepository;

    public MechanicSettingsController(MechanicSettingsService settingsService,
                                      UserRepository userRepository) {
        this.settingsService = settingsService;
        this.userRepository = userRepository;
    }

    // ── Settings (upsert / get) ───────────────────────────────────────────────

    @Operation(summary = "Save mechanic settings (create or update)",
               description = "Creates or fully replaces the mechanic's booking settings including job card type configuration.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings saved"),
        @ApiResponse(responseCode = "400", description = "Validation failed or business rule violated")
    })
    @PutMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicSettingsResponse>> saveSettings(
            @Valid @RequestBody MechanicSettingsRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicSettingsResponse response = settingsService.saveSettings(mechanic, request);
        return ResponseEntity.ok(MyApiResponse.success("Settings saved successfully", response));
    }

    @Operation(summary = "Get my settings")
    @GetMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicSettingsResponse>> getMySettings(
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(settingsService.getSettings(mechanic)));
    }

    @Operation(summary = "Get settings by mechanic ID")
    @GetMapping("/{mechanicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<MechanicSettingsResponse>> getSettingsByMechanicId(
            @PathVariable Long mechanicId) {

        return ResponseEntity.ok(MyApiResponse.success(
                settingsService.getSettingsByMechanicId(mechanicId)));
    }

    // ── Service settings CRUD ─────────────────────────────────────────────────

    @Operation(summary = "Add a service setting")
    @PostMapping("/services")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicServiceSettingResponse>> addService(
            @Valid @RequestBody MechanicServiceSettingRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicServiceSettingResponse response = settingsService.addServiceSetting(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Service added successfully", response));
    }

    @Operation(summary = "Update a service setting")
    @PutMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicServiceSettingResponse>> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody MechanicServiceSettingRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicServiceSettingResponse response =
                settingsService.updateServiceSetting(mechanic, serviceId, request);
        return ResponseEntity.ok(MyApiResponse.success("Service updated successfully", response));
    }

    @Operation(summary = "Delete a service setting")
    @DeleteMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteService(
            @PathVariable Long serviceId, Authentication authentication) {

        User mechanic = resolveUser(authentication);
        settingsService.deleteServiceSetting(mechanic, serviceId);
        return ResponseEntity.ok(MyApiResponse.success("Service deleted successfully", null));
    }

    @Operation(summary = "Get active services for a mechanic")
    @GetMapping("/{mechanicId}/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicServiceSettingResponse>>> getActiveServices(
            @PathVariable Long mechanicId) {

        return ResponseEntity.ok(MyApiResponse.success(
                settingsService.getActiveServiceSettings(mechanicId)));
    }

    // ── Service Slots CRUD (TYPE_4) ───────────────────────────────────────────

    @Operation(summary = "Add a service slot",
               description = "Adds a time-windowed service slot for TYPE_4 mechanics. "
                           + "Each slot is restricted to a specific service category.")
    @PostMapping("/service-slots")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicServiceSlotResponse>> addServiceSlot(
            @Valid @RequestBody MechanicServiceSlotRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicServiceSlotResponse response = settingsService.addServiceSlot(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Service slot added successfully", response));
    }

    @Operation(summary = "Update a service slot")
    @PutMapping("/service-slots/{slotId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicServiceSlotResponse>> updateServiceSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody MechanicServiceSlotRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicServiceSlotResponse response =
                settingsService.updateServiceSlot(mechanic, slotId, request);
        return ResponseEntity.ok(MyApiResponse.success("Service slot updated successfully", response));
    }

    @Operation(summary = "Delete a service slot")
    @DeleteMapping("/service-slots/{slotId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteServiceSlot(
            @PathVariable Long slotId, Authentication authentication) {

        User mechanic = resolveUser(authentication);
        settingsService.deleteServiceSlot(mechanic, slotId);
        return ResponseEntity.ok(MyApiResponse.success("Service slot deleted successfully", null));
    }

    @Operation(summary = "List service slots for a mechanic",
               description = "Returns enabled service slots for booking flow. Filters by restrictedCategory on the client side.")
    @GetMapping("/{mechanicId}/service-slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicServiceSlotResponse>>> getServiceSlots(
            @PathVariable Long mechanicId) {

        return ResponseEntity.ok(MyApiResponse.success(
                settingsService.getServiceSlots(mechanicId)));
    }

    // ── Technician Capacity CRUD (TYPE_3/4) ───────────────────────────────────

    @Operation(summary = "Add a technician",
               description = "Adds a technician with reserved hours. Effective capacity = SUM of active technicians' reserved hours.")
    @PostMapping("/technicians")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicTechnicianCapacityResponse>> addTechnician(
            @Valid @RequestBody MechanicTechnicianCapacityRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicTechnicianCapacityResponse response = settingsService.addTechnician(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Technician added successfully", response));
    }

    @Operation(summary = "Update a technician",
               description = "Update hours or toggle active status. Setting isActive=false marks the technician as absent.")
    @PutMapping("/technicians/{techId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicTechnicianCapacityResponse>> updateTechnician(
            @PathVariable Long techId,
            @Valid @RequestBody MechanicTechnicianCapacityRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        MechanicTechnicianCapacityResponse response =
                settingsService.updateTechnician(mechanic, techId, request);
        return ResponseEntity.ok(MyApiResponse.success("Technician updated successfully", response));
    }

    @Operation(summary = "Remove a technician")
    @DeleteMapping("/technicians/{techId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteTechnician(
            @PathVariable Long techId, Authentication authentication) {

        User mechanic = resolveUser(authentication);
        settingsService.deleteTechnician(mechanic, techId);
        return ResponseEntity.ok(MyApiResponse.success("Technician removed successfully", null));
    }

    @Operation(summary = "List own technicians")
    @GetMapping("/technicians")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<MechanicTechnicianCapacityResponse>>> getTechnicians(
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(settingsService.getTechnicians(mechanic)));
    }

    // ── Daily Quota Override CRUD ─────────────────────────────────────────────

    @Operation(summary = "Set a daily quota override",
               description = "Temporarily override capacity limits for a specific date (e.g. festival rush).")
    @PostMapping("/daily-override")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<DailyQuotaOverrideResponse>> addOverride(
            @Valid @RequestBody DailyQuotaOverrideRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        DailyQuotaOverrideResponse response = settingsService.addOverride(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Override created successfully", response));
    }

    @Operation(summary = "Update a daily override")
    @PutMapping("/daily-override/{overrideId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<DailyQuotaOverrideResponse>> updateOverride(
            @PathVariable Long overrideId,
            @Valid @RequestBody DailyQuotaOverrideRequest request,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        DailyQuotaOverrideResponse response =
                settingsService.updateOverride(mechanic, overrideId, request);
        return ResponseEntity.ok(MyApiResponse.success("Override updated successfully", response));
    }

    @Operation(summary = "Remove a daily override")
    @DeleteMapping("/daily-override/{overrideId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteOverride(
            @PathVariable Long overrideId, Authentication authentication) {

        User mechanic = resolveUser(authentication);
        settingsService.deleteOverride(mechanic, overrideId);
        return ResponseEntity.ok(MyApiResponse.success("Override removed successfully", null));
    }

    @Operation(summary = "List own daily overrides")
    @GetMapping("/daily-overrides")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<DailyQuotaOverrideResponse>>> getOverrides(
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(settingsService.getOverrides(mechanic)));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
