package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.ServiceArriveRequest;
import com.alvexo.bookingapp.dto.request.ServiceBulkStatusRequest;
import com.alvexo.bookingapp.dto.request.ServiceCancelRequest;
import com.alvexo.bookingapp.dto.request.ServiceStatusChangeRequest;
import com.alvexo.bookingapp.dto.response.BulkStatusResultResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.NextJobCardNumberResponse;
import com.alvexo.bookingapp.dto.response.ServiceBookingInfoResponse;
import com.alvexo.bookingapp.dto.response.ServiceCancelResponse;
import com.alvexo.bookingapp.dto.response.ServiceTodayResponse;
import com.alvexo.bookingapp.dto.response.ServiceVehicleResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ServiceDeskTodayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Service Desk - Today", description = "SERVICE_DESK_API_SPEC.md §1 — the current service day workspace.")
@RestController
@RequestMapping("/api/service-desk")
@PreAuthorize("hasRole('MECHANIC')")
public class ServiceDeskTodayController {

    private final ServiceDeskTodayService todayService;
    private final UserRepository userRepository;

    public ServiceDeskTodayController(ServiceDeskTodayService todayService, UserRepository userRepository) {
        this.todayService = todayService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get today's service workspace", description = "Carry-over + today + cancelled partitions, plus matrix counters. Future dates are rejected.")
    @GetMapping("/today")
    public ResponseEntity<MyApiResponse<ServiceTodayResponse>> getToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(MyApiResponse.success(todayService.getToday(resolveUser(authentication), resolvedDate)));
    }

    @Operation(summary = "Get booking info for the Selected Vehicle panel")
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<MyApiResponse<ServiceBookingInfoResponse>> getBookingInfo(
            @PathVariable String bookingId, Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(
                todayService.getBookingInfo(resolveUser(authentication), bookingId)));
    }

    @Operation(summary = "Change booking status", description = "Only Pending/Completed targets — use /arrive for Arrived and /cancel for Cancelled.")
    @PutMapping("/bookings/{bookingId}/status")
    public ResponseEntity<MyApiResponse<ServiceVehicleResponse>> changeStatus(
            @PathVariable String bookingId,
            @Valid @RequestBody ServiceStatusChangeRequest request,
            Authentication authentication) {
        ServiceVehicleResponse response =
                todayService.changeStatus(resolveUser(authentication), bookingId, request.getStatus());
        return ResponseEntity.ok(MyApiResponse.success("Status updated", response));
    }

    @Operation(summary = "Mark Arrived and issue a job card", description = "Rejects with a duplicate-job-card error if the number is already in use.")
    @PutMapping("/bookings/{bookingId}/arrive")
    public ResponseEntity<MyApiResponse<ServiceVehicleResponse>> arrive(
            @PathVariable String bookingId,
            @Valid @RequestBody ServiceArriveRequest request,
            Authentication authentication) {
        ServiceVehicleResponse response =
                todayService.arrive(resolveUser(authentication), bookingId, request.getJobCardNumber());
        return ResponseEntity.ok(MyApiResponse.success("Vehicle marked Arrived", response));
    }

    @Operation(summary = "Cancel a booking", description = "Flags a ₹30 Service Reliability Adjustment when past the reschedule cutoff.")
    @PutMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<MyApiResponse<ServiceCancelResponse>> cancel(
            @PathVariable String bookingId,
            @Valid @RequestBody ServiceCancelRequest request,
            Authentication authentication) {
        ServiceCancelResponse response =
                todayService.cancel(resolveUser(authentication), bookingId, request.getMessage());
        return ResponseEntity.ok(MyApiResponse.success("Booking cancelled", response));
    }

    @Operation(summary = "Bulk status change", description = "Applies to Arrived/Pending bookings only; disallowed items are skipped and reported per-item.")
    @PostMapping("/bookings/bulk-status")
    public ResponseEntity<MyApiResponse<BulkStatusResultResponse>> bulkStatus(
            @Valid @RequestBody ServiceBulkStatusRequest request, Authentication authentication) {
        BulkStatusResultResponse response = todayService.bulkStatus(
                resolveUser(authentication), request.getBookingIds(), request.getStatus());
        return ResponseEntity.ok(MyApiResponse.success(response));
    }

    @Operation(summary = "Compute the next job card number", description = "Server-computed from the mechanic's job card config; editable before saving via /arrive.")
    @GetMapping("/next-job-card-number")
    public ResponseEntity<MyApiResponse<NextJobCardNumberResponse>> nextJobCardNumber(Authentication authentication) {
        return ResponseEntity.ok(MyApiResponse.success(todayService.nextJobCardNumber(resolveUser(authentication))));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
