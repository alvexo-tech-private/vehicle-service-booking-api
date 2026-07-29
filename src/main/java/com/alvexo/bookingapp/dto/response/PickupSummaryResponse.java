package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** GET /api/service-desk/pickups (§3.2/§3.3). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupSummaryResponse {
    private long total;
    private long assigned;
    private long unassigned;
    private LocalDateTime lastUpdated;
    private List<PickupVehicleResponse> vehicles;
}
