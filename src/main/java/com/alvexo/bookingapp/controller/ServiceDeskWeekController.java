package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.ServiceCancelRequest;
import com.alvexo.bookingapp.dto.request.WeekRescheduleRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.ServiceCancelResponse;
import com.alvexo.bookingapp.dto.response.WeekDayResponse;
import com.alvexo.bookingapp.dto.response.WeekRescheduleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ServiceDeskWeekService;
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

@Tag(name = "Service Desk - Service Week", description = "SERVICE_DESK_API_SPEC.md §2 — future dates only, today excluded.")
@RestController
@RequestMapping("/api/service-desk/service-week")
@PreAuthorize("hasRole('MECHANIC')")
public class ServiceDeskWeekController {

    private final ServiceDeskWeekService weekService;
    private final UserRepository userRepository;

    public ServiceDeskWeekController(ServiceDeskWeekService weekService, UserRepository userRepository) {
        this.weekService = weekService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get future service days with vehicles + capacity", description = "Defaults to a 7-day window starting tomorrow when from/to are omitted.")
    @GetMapping
    public ResponseEntity<MyApiResponse<List<WeekDayResponse>>> getServiceWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(
                weekService.getServiceWeek(resolveUser(authentication), from, to)));
    }

    @Operation(summary = "Reschedule a vehicle to another date", description = "Still allowed when the target date is over capacity; response flags overCapacity.")
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<MyApiResponse<WeekRescheduleResponse>> reschedule(
            @PathVariable String id,
            @Valid @RequestBody WeekRescheduleRequest request,
            Authentication authentication) {
        WeekRescheduleResponse response =
                weekService.reschedule(resolveUser(authentication), id, request.getToDate());
        return ResponseEntity.ok(MyApiResponse.success("Vehicle rescheduled", response));
    }

    @Operation(summary = "Cancel a future booking", description = "Flags a ₹30 Service Reliability Adjustment when past the reschedule cutoff.")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<MyApiResponse<ServiceCancelResponse>> cancel(
            @PathVariable String id,
            @Valid @RequestBody ServiceCancelRequest request,
            Authentication authentication) {
        ServiceCancelResponse response = weekService.cancel(resolveUser(authentication), id, request.getMessage());
        return ResponseEntity.ok(MyApiResponse.success("Booking cancelled", response));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
