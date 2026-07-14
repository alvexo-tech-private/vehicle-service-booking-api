package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.EarningsResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ServiceDeskEarningsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Service Desk - Earnings", description = "SERVICE_DESK_API_SPEC.md §5 — today's money view, read-only.")
@RestController
@RequestMapping("/api/service-desk/earnings")
@PreAuthorize("hasRole('MECHANIC')")
public class ServiceDeskEarningsController {

    private final ServiceDeskEarningsService earningsService;
    private final UserRepository userRepository;

    public ServiceDeskEarningsController(ServiceDeskEarningsService earningsService, UserRepository userRepository) {
        this.earningsService = earningsService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get today's earnings", description = "Advances (+), cancellation deductions (-), and net payable, plus per-vehicle entries.")
    @GetMapping
    public ResponseEntity<MyApiResponse<EarningsResponse>> getEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(earningsService.getEarnings(resolveUser(authentication), date)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
