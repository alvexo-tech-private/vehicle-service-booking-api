package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** GET /api/service-desk/drops (§4.2/§4.3). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropSummaryResponse {
    private long total;
    private long assigned;
    private long delivered;
    private LocalDateTime lastUpdated;
    private List<DropVehicleResponse> vehicles;
}
