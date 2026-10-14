package com.alvexo.bookingapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvexo.bookingapp.dto.request.ReminderNotifyRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.ReminderNotifyResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.ReminderCycleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Workshop Reminders",
     description = "BACKEND_REQUIREMENTS_SERVICE_REMINDER_128.md — sends first/second service reminders per reminder cycle.")
@RestController
@RequestMapping("/api/workshop/reminders")
@PreAuthorize("hasRole('MECHANIC')")
public class WorkshopReminderController {

    private final ReminderCycleService reminderCycleService;
    private final UserRepository userRepository;

    public WorkshopReminderController(ReminderCycleService reminderCycleService, UserRepository userRepository) {
        this.reminderCycleService = reminderCycleService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Send reminders for a batch of vehicles",
               description = "Idempotent per (reminderCycleId, stage) — resending an already-sent stage returns "
                            + "ALREADY_SENT instead of sending again. Returns per-row accepted/skipped/failed results.")
    @PostMapping("/notify")
    public ResponseEntity<MyApiResponse<ReminderNotifyResponse>> notify(
            @Valid @RequestBody ReminderNotifyRequest request,
            Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReminderNotifyResponse response = reminderCycleService.notify(mechanic, request);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }
}
