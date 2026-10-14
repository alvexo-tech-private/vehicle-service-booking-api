package com.alvexo.bookingapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.RestorePointRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.RestorePointSummaryResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicSettingsRestorePointService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Mechanic Settings Restore Points",
     description = "WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.2 — snapshot/restore capacity + configuration settings.")
@RestController
@RequestMapping("/api/mechanic-settings/restore-points")
@PreAuthorize("hasRole('MECHANIC')")
public class MechanicSettingsRestorePointController {

    private final MechanicSettingsRestorePointService restorePointService;
    private final UserRepository userRepository;

    public MechanicSettingsRestorePointController(
            MechanicSettingsRestorePointService restorePointService, UserRepository userRepository) {
        this.restorePointService = restorePointService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Save a settings snapshot", description = "Snapshots the mechanic's currently-saved capacity + configuration settings under the given name.")
    @PostMapping
    public ResponseEntity<MyApiResponse<RestorePointSummaryResponse>> save(
            @Valid @RequestBody RestorePointRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        RestorePointSummaryResponse response = restorePointService.save(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Restore point saved", response));
    }

    @Operation(summary = "List settings snapshots", description = "Newest first.")
    @GetMapping
    public ResponseEntity<MyApiResponse<List<RestorePointSummaryResponse>>> list(Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.ok(MyApiResponse.success(restorePointService.list(mechanic)));
    }

    @Operation(summary = "Restore from a snapshot", description = "Re-applies the snapshot's settings + configuration as the active values.")
    @PostMapping("/{id}/restore")
    public ResponseEntity<MyApiResponse<Void>> restore(
            @PathVariable Long id,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        restorePointService.restore(mechanic, id);
        return ResponseEntity.ok(MyApiResponse.success("Settings restored", null));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
