package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PUT /api/service-desk/service-week/{id}/reschedule (§2.4) response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekRescheduleResponse {
    private WeekVehicleResponse vehicle;
    /** True when the target date was already at/over capacity — the reschedule still succeeded. */
    private boolean overCapacity;
}
