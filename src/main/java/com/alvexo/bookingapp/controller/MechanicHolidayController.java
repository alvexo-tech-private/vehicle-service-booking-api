package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.request.MechanicHolidayRequest;
import com.alvexo.bookingapp.dto.response.MechanicHolidayResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicHolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mechanic Holidays", description = "Per-date Holiday/Pause markers that block new bookings for that date.")
@RestController
@RequestMapping("/api/mechanic-settings/holidays")
public class MechanicHolidayController {

    private final MechanicHolidayService holidayService;
    private final UserRepository userRepository;

    public MechanicHolidayController(MechanicHolidayService holidayService, UserRepository userRepository) {
        this.holidayService = holidayService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Mark a date as Holiday or Pause")
    @PostMapping
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicHolidayResponse>> createHoliday(
            @Valid @RequestBody MechanicHolidayRequest request,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MyApiResponse.success("Holiday/pause created", holidayService.createHoliday(mechanic, request)));
    }

    @Operation(summary = "Remove a Holiday/Pause marker")
    @DeleteMapping("/{holidayId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<Void>> deleteHoliday(
            @PathVariable Long holidayId,
            Authentication authentication) {
        User mechanic = resolveUser(authentication);
        holidayService.deleteHoliday(mechanic, holidayId);
        return ResponseEntity.ok(MyApiResponse.success("Holiday/pause removed", null));
    }

    @Operation(summary = "List holidays/pauses for a mechanic in a date range")
    @GetMapping("/{mechanicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyApiResponse<List<MechanicHolidayResponse>>> getHolidays(
            @PathVariable Long mechanicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(MyApiResponse.success(holidayService.getHolidays(mechanicId, from, to)));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
