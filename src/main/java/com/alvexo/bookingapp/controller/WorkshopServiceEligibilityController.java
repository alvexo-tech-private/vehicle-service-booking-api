package com.alvexo.bookingapp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.ServiceEligibilityRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.ServiceEligibilityEntryResponse;
import com.alvexo.bookingapp.dto.response.ServiceEligibilityResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.WorkshopServiceEligibilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Workshop Service Eligibility",
     description = "WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.1 — which vehicle makes/categories a workshop services.")
@RestController
@RequestMapping("/api/workshop/service-eligibility")
@PreAuthorize("hasRole('MECHANIC')")
public class WorkshopServiceEligibilityController {

    private final WorkshopServiceEligibilityService eligibilityService;
    private final UserRepository userRepository;

    public WorkshopServiceEligibilityController(
            WorkshopServiceEligibilityService eligibilityService, UserRepository userRepository) {
        this.eligibilityService = eligibilityService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get my service eligibility", description = "Returns the logged-in workshop's configured vehicle-make coverage per fuel type/category.")
    @GetMapping
    public ResponseEntity<MyApiResponse<ServiceEligibilityResponse>> getMine(Authentication authentication) {
        User mechanic = resolveUser(authentication);
        List<ServiceEligibilityEntryResponse> entries = eligibilityService.getEligibility(mechanic.getId());
        return ResponseEntity.ok(MyApiResponse.success(new ServiceEligibilityResponse(entries)));
    }

    @Operation(summary = "Save my service eligibility", description = "Upserts one or more (fuelType, serviceCategory) coverage entries.")
    @PostMapping
    public ResponseEntity<MyApiResponse<ServiceEligibilityResponse>> save(
            @Valid @RequestBody ServiceEligibilityRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        List<ServiceEligibilityEntryResponse> entries =
                eligibilityService.saveEligibility(mechanic, request.getEntries());
        return ResponseEntity.ok(MyApiResponse.success("Service eligibility saved", new ServiceEligibilityResponse(entries)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
