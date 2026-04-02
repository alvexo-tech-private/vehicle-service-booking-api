package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.request.WeeklyAvailabilityRequest;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.SlotResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicService;
import com.alvexo.bookingapp.util.MobileNumberUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mechanics", description = "Mechanic availability management, slot discovery, and nearby search.")
@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {

	@Autowired
	private MechanicService mechanicService;   
	
	@Autowired
	private UserRepository userRepository;
    
    // ── Availability template management (mechanic only) ──────────────────────

    @Operation(summary = "Add availability template",
        description = "Mechanic sets working hours for a day of the week. " +
                      "slotDurationMinutes controls how long each booking slot is (default 60).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Availability added"),
        @ApiResponse(responseCode = "400", description = "Validation failed or not a mechanic")
    })
    @PostMapping("/availability")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<AvailabilityResponse>> addAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.addAvailability(mechanic, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Availability added successfully", response));
    }
    
    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('MECHANIC', 'VEHICLE_USER')")   
    public ResponseEntity<MyApiResponse<List<AvailabilityResponse>>> getMyAvailability(
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<AvailabilityResponse> availability = mechanicService.getMechanicAvailability(mechanic);
        return ResponseEntity.ok(MyApiResponse.success(availability));
    }

    /**
     * @apiNote Get Mechanic by Mobile Number
     * @param authentication the security context of the authenticated user
     * @param mobileNumber the mobile number of the mechanic to look up
     * @return Mechanic by Mobile Numbeer
     */
    @GetMapping("/availability/{mobileNumber}")
    @PreAuthorize("hasRole('VEHICLE_USER')")
    public ResponseEntity<MyApiResponse<List<AvailabilityResponse>>> getMechanicAvailabilityByMobileNumber(@PathVariable String mobileNumber, Authentication authentication) {
        userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User mechanicByPhone = userRepository.findByMobileNumber(MobileNumberUtil.normalize(mobileNumber)).orElseThrow(() -> new ResourceNotFoundException("Mechanic not found with " + mobileNumber));
        List<AvailabilityResponse> availability = mechanicService.getMechanicAvailability(mechanicByPhone);
        return ResponseEntity.ok(MyApiResponse.success(availability));
    }
    
    @PutMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<AvailabilityResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        AvailabilityResponse response = mechanicService.updateAvailability(id, request, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Availability updated successfully", response));
    }
    
    @DeleteMapping("/availability/{id}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteAvailability(
            @PathVariable Long id,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        mechanicService.deleteAvailability(id, mechanic);
        return ResponseEntity.ok(MyApiResponse.success("Availability deleted successfully", null));
    }
    
    // ── Weekly availability (one call for all days) ───────────────────────────

    @Operation(
        summary = "Set availability for the whole week",
        description = """
            Sets or replaces availability for multiple days in a single request.
            Include 1 to 7 days. Days not in the list are left unchanged.

            **Rules:**
            - Each day entry follows the same structure as the single-day endpoint.
            - If a rule already exists for a day it is replaced in-place.
            - If no rule exists for a day a new one is created.
            - Set isAvailable=false to mark a day off without deleting the rule.
            - Each day can have its own break windows (lunch, prayer break, etc).

            **Sample payload — Mon to Fri with lunch break, half-day Saturday, Sunday off:**
            ```json
            {
              "days": [
                { "dayOfWeek":"MONDAY",    "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
                  "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
                { "dayOfWeek":"TUESDAY",   "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
                  "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
                { "dayOfWeek":"WEDNESDAY", "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
                  "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
                { "dayOfWeek":"THURSDAY",  "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
                  "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
                { "dayOfWeek":"FRIDAY",    "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
                  "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
                { "dayOfWeek":"SATURDAY",  "startTime":"09:00","endTime":"13:00","slotDurationMinutes":60 },
                { "dayOfWeek":"SUNDAY",    "startTime":"09:00","endTime":"18:00","isAvailable":false }
              ]
            }
            ```
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Week availability set successfully"),
        @ApiResponse(responseCode = "400", description = "Duplicate days or validation failed")
    })
    @PostMapping("/availability/week")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<List<AvailabilityResponse>>> setWeeklyAvailability(
            @Valid @RequestBody WeeklyAvailabilityRequest request,
            Authentication authentication) {
    	
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<AvailabilityResponse> responses =
                mechanicService.setWeeklyAvailability(mechanic, request);
        return ResponseEntity.ok(MyApiResponse.success(
                "Weekly availability set for " + responses.size() + " day(s)", responses));
    }

    // ── Slot discovery (any authenticated user) ───────────────────────────────

    @Operation(
        summary = "Get available slots for a mechanic on a date",
        description = """
            Returns all bookable time slots for a mechanic on the requested date.
            Computed on-demand — already-booked slots are excluded automatically.

            **How to make multiple bookings in the same day:**
            1. Call GET /api/mechanics/{id}/slots?date=2025-07-15 for each mechanic you want.
            2. Pick a slot from the response (use the `scheduledDateTime` field).
            3. Call POST /api/bookings with that scheduledDateTime.
            4. Repeat for a different mechanic or a different slot time.

            A vehicle user can have multiple bookings in the same day with different
            mechanics, or with the same mechanic at non-overlapping times.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of available slots (empty if mechanic unavailable that day)"),
        @ApiResponse(responseCode = "400", description = "Past date or invalid mechanic ID")
    })
    @GetMapping("/{mechanicId}/slots")
    public ResponseEntity<MyApiResponse<List<SlotResponse>>> getAvailableSlots(
            @PathVariable Long mechanicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SlotResponse> slots = mechanicService.getAvailableSlots(mechanicId, date);
        String message = slots.isEmpty()
                ? "No available slots on " + date
                : slots.size() + " slot(s) available on " + date;
        return ResponseEntity.ok(MyApiResponse.success(message, slots));
    }

    // ── Nearby search ─────────────────────────────────────────────────────────

    @Operation(summary = "Find nearby mechanics",
        description = "Returns active mechanics within a given radius (km) of provided coordinates.")
    @GetMapping("/nearby")
    public ResponseEntity<MyApiResponse<List<UserResponse>>> findNearbyMechanics(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        return ResponseEntity.ok(MyApiResponse.success(
                mechanicService.findNearbyMechanics(latitude, longitude, radiusKm)));
    }
}
