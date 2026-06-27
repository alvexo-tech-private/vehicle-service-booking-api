package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.AvailabilityCheckResponse;
import com.alvexo.bookingapp.dto.response.DashboardMetricsResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Mechanic Dashboard",
     description = "Real-time booking metrics and availability checks for mechanics and riders.")
@RestController
public class MechanicDashboardController {

    private final MechanicDashboardService dashboardService;
    private final UserRepository userRepository;

    public MechanicDashboardController(MechanicDashboardService dashboardService,
                                        UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Check booking availability",
               description = "Returns per-category availability and slot status for a mechanic on a date. "
                           + "Used by rider UI to grey out unavailable dates/services.")
    @GetMapping("/api/bookings/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<AvailabilityCheckResponse>> checkAvailability(
            @RequestParam Long mechanicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AvailabilityCheckResponse response = dashboardService.getAvailability(mechanicId, date);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    @Operation(summary = "Today's dashboard metrics",
               description = "Real-time counters for the mechanic's home screen: "
                           + "booked count, capacity usage, auto/manual/walk-in breakdown, slot utilization.")
    @GetMapping("/api/mechanic-dashboard/today")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<DashboardMetricsResponse>> getTodayMetrics(
            Authentication authentication) {

        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DashboardMetricsResponse response = dashboardService.getTodayMetrics(mechanic);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }
}
