package com.alvexo.bookingapp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.AvailabilityRequest;
import com.alvexo.bookingapp.dto.request.WeeklyAvailabilityRequest;
import com.alvexo.bookingapp.dto.response.AvailabilityResponse;
import com.alvexo.bookingapp.dto.response.MechanicSearchResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.SlotResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
 
    // ── Unified mechanic search ───────────────────────────────────────────────

    @Operation(
        summary = "Search mechanics",
        description = """
            Returns active mechanics matching exactly one of the supported search dimensions.
            Exactly one parameter must be provided per request — mixing parameters returns 400.

            | Parameter  | Type   | Description                                              |
            |------------|--------|----------------------------------------------------------|
            | `city`     | String | City name (case-insensitive). Combine with `area` to narrow to a neighbourhood. |
            | `area`     | String | Neighbourhood within the city — only evaluated when `city` is present. |
            | `mobile`   | String | Exact 10-digit mobile number registered to the mechanic. |
            | `pinCode`  | String | Exact 6-digit postal / PIN code of the mechanic's workshop. |

            **Examples:**
            - `/api/mechanics/search?city=Chennai` — all mechanics in Chennai
            - `/api/mechanics/search?city=Chennai&area=Anna Nagar` — mechanics in Anna Nagar, Chennai
            - `/api/mechanics/search?mobile=9000000005` — mechanic with that mobile number
            - `/api/mechanics/search?pinCode=600040` — mechanics whose workshop PIN code is 600040
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of matching mechanics (empty list if none found)"),
        @ApiResponse(responseCode = "400", description = "No search parameter provided, or more than one provided simultaneously")
    })
    @GetMapping("/search")
    public ResponseEntity<MyApiResponse<List<MechanicSearchResponse>>> searchMechanics(
            @Parameter(description = "City name (case-insensitive)", example = "Chennai")
            @RequestParam(required = false) String city,

            @Parameter(description = "Neighbourhood within the city — only used alongside city", example = "Anna Nagar")
            @RequestParam(required = false) String area,

            @Parameter(description = "Exact mobile number of the mechanic", example = "9000000005")
            @RequestParam(required = false) String mobile,

            @Parameter(description = "Exact postal / PIN code of the mechanic's workshop", example = "600040")
            @RequestParam(required = false) String pinCode) {

        List<MechanicSearchResponse> mechanics =
                mechanicService.searchMechanics(city, area, mobile, pinCode);

        String searchLabel = resolveSearchLabel(city, area, mobile, pinCode);
        String message = mechanics.isEmpty()
                ? "No mechanics found for " + searchLabel
                : mechanics.size() + " mechanic(s) found for " + searchLabel;

        return ResponseEntity.ok(MyApiResponse.success(message, mechanics));
    }

    private String resolveSearchLabel(String city, String area, String mobile, String pinCode) {
        if (city != null && !city.isBlank()) {
            return "city: " + city.trim() + (area != null && !area.isBlank() ? ", area: " + area.trim() : "");
        }
        if (mobile != null && !mobile.isBlank()) {
            return "mobile: " + mobile.trim();
        }
        if (pinCode != null && !pinCode.isBlank()) {
            return "pin code: " + pinCode.trim();
        }
        return "the given criteria";
    }
}
