package com.alvexo.bookingapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import com.alvexo.bookingapp.dto.request.BookingCancelRequest;
import com.alvexo.bookingapp.dto.request.BookingRequest;
import com.alvexo.bookingapp.dto.request.WalkInBookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Bookings", description = "Create and manage service bookings between vehicle users and mechanics. Requires JWT token.")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "Create a booking", description = "Vehicle user creates a new service booking with a mechanic.")
    @PostMapping
    public ResponseEntity<MyApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        BookingResponse response = bookingService.createBooking(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Booking created successfully", response));
    }
    
    @Operation(summary = "Get my bookings", description = "Returns paginated list of bookings for the currently authenticated user.")
    @GetMapping("/my-bookings")
    public ResponseEntity<MyApiResponse<Page<BookingResponse>>> getMyBookings(
            Pageable pageable,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<BookingResponse> bookings = bookingService.getUserBookings(user, pageable);
        return ResponseEntity.ok(MyApiResponse.success(bookings));
    }
    
    @Operation(summary = "Get booking by ID", description = "Returns full details of a specific booking. User must be the customer or the mechanic.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        BookingResponse booking = bookingService.getBookingById(id, user);
        return ResponseEntity.ok(MyApiResponse.success(booking));
    }
    
    @Operation(summary = "Update booking status", description = "Mechanic or admin updates the status of a booking (e.g. CONFIRMED, COMPLETED, CANCELLED).")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MECHANIC', 'ADMINISTRATOR')")   
    public ResponseEntity<MyApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus status,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        BookingResponse response = bookingService.updateBookingStatus(id, status, user);
        return ResponseEntity.ok(MyApiResponse.success("Booking status updated", response));
    }

    @Operation(summary = "Cancel my booking",
               description = "Vehicle user cancels a booking they created. Only PENDING or CONFIRMED bookings can be cancelled.")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('VEHICLE_USER')")
    public ResponseEntity<MyApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingCancelRequest request,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BookingResponse response = bookingService.cancelBookingAsRider(id, user, request.getReason());
        return ResponseEntity.ok(MyApiResponse.success("Booking cancelled", response));
    }

    @Operation(summary = "Record a walk-in booking",
               description = "Mechanic records a walk-in customer at the workshop. Confirmed immediately, " +
                              "tagged channel = WALK_IN, and still subject to the mechanic's capacity/advance rules.")
    @PostMapping("/walk-in")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<BookingResponse>> createWalkInBooking(
            @Valid @RequestBody WalkInBookingRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BookingResponse response = bookingService.createWalkInBooking(request, mechanic);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Walk-in booking recorded successfully", response));
    }

    @Operation(summary = "Reschedule a booking",
               description = "The assigned mechanic or the booking's own vehicle user moves it to a new date/time "
                            + "(RIDER_BOOKING_TO_WORKSHOP.md §5: rider can \"modify\"), subject to the same conflict checks as a new booking.")
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('MECHANIC', 'VEHICLE_USER')")
    public ResponseEntity<MyApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable Long id,
            @RequestParam("scheduledDateTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime newScheduledDateTime,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BookingResponse response = bookingService.rescheduleBooking(id, newScheduledDateTime, user);
        return ResponseEntity.ok(MyApiResponse.success("Booking rescheduled successfully", response));
    }

    @Operation(summary = "Issue job card manually",
               description = "For mechanics with autoIssue = false: issues a job card for an already-CONFIRMED booking.")
    @PutMapping("/{id}/issue-job-card")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<BookingResponse>> issueJobCard(
            @PathVariable Long id,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BookingResponse response = bookingService.issueJobCard(id, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Job card issued", response));
    }
}
