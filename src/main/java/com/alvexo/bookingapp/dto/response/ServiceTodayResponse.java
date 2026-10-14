package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** GET /api/service-desk/today (§1.3) — pre-partitioned so the client doesn't have to derive sections. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTodayResponse {
    private List<ServiceVehicleResponse> carryOver;
    private List<ServiceVehicleResponse> today;
    private List<ServiceVehicleResponse> cancelled;

    /** Matrix counters (§1.9) over the "today" partition only. */
    private long completed;
    private long arrived;
    private long pending;
    private long scheduled;
}
