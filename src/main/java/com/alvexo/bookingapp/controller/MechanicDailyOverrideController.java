package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.MechanicDailyOverrideRequest;
import com.alvexo.bookingapp.dto.response.MechanicDailyOverrideResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicDailyOverrideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mechanic Daily Overrides",
     description = "Per-date capacity/advance overrides (Home 'One-Day Capacity & Advance Change').")
@RestController
@RequestMapping("/api/mechanic-settings/daily-overrides")
public class MechanicDailyOverrideController {

    private final MechanicDailyOverrideService overrideService;
    private final UserRepository userRepository;

    public MechanicDailyOverrideController(MechanicDailyOverrideService overrideService,
                                             UserRepository userRepository) {
        this.overrideService = overrideService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Save a daily override (create or update by date)")
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicDailyOverrideResponse>> saveOverride(
            @Valid @RequestBody MechanicDailyOverrideRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success("Daily override saved successfully",
                overrideService.saveOverride(mechanic, request)));
    }

    @Operation(summary = "Delete a daily override")
    @DeleteMapping("/{overrideId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteOverride(
            @PathVariable Long overrideId,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        overrideService.deleteOverride(mechanic, overrideId);
        return ResponseEntity.ok(MyApiResponse.success("Daily override removed", null));
    }

    @Operation(summary = "List daily overrides for a mechanic in a date range")
    @GetMapping("/{mechanicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicDailyOverrideResponse>>> getOverrides(
            @PathVariable Long mechanicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(MyApiResponse.success(overrideService.getOverrides(mechanicId, from, to)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
