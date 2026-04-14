package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.MechanicServiceSettingRequest;
import com.alvexo.bookingapp.dto.request.MechanicSettingsRequest;
import com.alvexo.bookingapp.dto.response.MechanicServiceSettingResponse;
import com.alvexo.bookingapp.dto.response.MechanicSettingsResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
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
     description = "Manage mechanic booking settings: capacity mode, reporting times, advance payment, and service catalogue.")
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

    @Operation(
        summary = "Save mechanic settings (create or update)",
        description = """
            Creates or fully replaces the mechanic's booking settings.

            **Capacity modes:**
            - `reserveCapacity = false` → Vehicle-count mode. Bookings are accepted until
              `maxVehiclesPerDay` is reached for the day.
            - `reserveCapacity = true`  → Hour-slot mode. Bookings are accepted until the
              sum of service durations reaches `fullDayCapacityHours`.
              `expressReportingTime` is required in this mode.

            **Advance payment:**
            - `advanceEnabled = true` requires `advanceAmount`.

            **Service settings:**
            - Optional `serviceSettings` array replaces all existing service definitions.
              Omit the array to leave existing services unchanged.

            **Job card serial:**
            - `jobCardSerialPrefix` (e.g. "0101") is used to generate job card numbers:
              `{prefix}{YYMMDD}{dailySeq}` — e.g. `010125040801`.
            """
    )
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

    @Operation(summary = "Get my settings", description = "Returns the authenticated mechanic's current settings.")
    @GetMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicSettingsResponse>> getMySettings(
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(settingsService.getSettings(mechanic)));
    }

    @Operation(summary = "Get settings by mechanic ID",
               description = "Used by booking flow to load a mechanic's settings. Accessible by any authenticated user.")
    @GetMapping("/{mechanicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<MechanicSettingsResponse>> getSettingsByMechanicId(
            @PathVariable Long mechanicId) {

        return ResponseEntity.ok(MyApiResponse.success(
                settingsService.getSettingsByMechanicId(mechanicId)));
    }

    // ── Service settings (individual CRUD) ────────────────────────────────────

    @Operation(summary = "Add a single service setting",
               description = "Adds one service to the mechanic's service catalogue.")
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

    @Operation(summary = "Delete a service setting",
               description = "Permanently removes a service from the catalogue.")
    @DeleteMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteService(
            @PathVariable Long serviceId,
            Authentication authentication) {

        User mechanic = resolveUser(authentication);
        settingsService.deleteServiceSetting(mechanic, serviceId);
        return ResponseEntity.ok(MyApiResponse.success("Service deleted successfully", null));
    }

    @Operation(summary = "Get active services for a mechanic",
               description = "Returns active services visible to customers during booking. Ordered by displayOrder.")
    @GetMapping("/{mechanicId}/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicServiceSettingResponse>>> getActiveServices(
            @PathVariable Long mechanicId) {

        return ResponseEntity.ok(MyApiResponse.success(
                settingsService.getActiveServiceSettings(mechanicId)));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
