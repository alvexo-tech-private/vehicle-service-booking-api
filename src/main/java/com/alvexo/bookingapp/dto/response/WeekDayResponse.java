package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.CapacityBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** GET /api/service-desk/service-week (§2.3) — one entry per future date. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekDayResponse {
    private LocalDate date;
    private int offset;
    private DayOfWeek dow;
    private Integer capacity;
    private long riderBooked;
    private long booked;
    private CapacityBand band;
    private List<WeekVehicleResponse> vehicles;
}
