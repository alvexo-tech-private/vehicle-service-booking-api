package com.alvexo.bookingapp.controller;

import com.alvexo.bookingapp.dto.response.MechanicDashboardResponse;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.MechanicDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mechanic Dashboard", description = "Home dashboard counters for the authenticated mechanic.")
@RestController
@RequestMapping("/api/mechanic-dashboard")
public class MechanicDashboardController {

    private final MechanicDashboardService dashboardService;
    private final UserRepository userRepository;

    public MechanicDashboardController(MechanicDashboardService dashboardService,
                                        UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @Operation(
        summary = "Get today's dashboard",
        description = """
            Returns the counters that drive the Home dashboard for the
            authenticated mechanic's current Workshop Capacity Level:
            confirmed/issued/pending counts, capacity utilization, the
            per-service F/I/P breakdown, and (Level 4 only) Slot 1/2 summaries.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard loaded"),
        @ApiResponse(responseCode = "404", description = "Mechanic settings not configured yet")
    })
    @GetMapping("/today")
    @PreAuthorize("hasRole('MECHANIC')")
    public ResponseEntity<MyApiResponse<MechanicDashboardResponse>> getToday(Authentication authentication) {
        User mechanic = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(MyApiResponse.success(dashboardService.getTodayDashboard(mechanic)));
    }
}
