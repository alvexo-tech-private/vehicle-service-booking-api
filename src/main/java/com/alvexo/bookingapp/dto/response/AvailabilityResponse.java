package com.alvexo.bookingapp.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

import com.alvexo.bookingapp.model.DayOfWeek;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private Long id;
    private DayOfWeek dayOfWeek;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Boolean isAvailable;
    private Integer slotDurationMinutes;
    private Integer maxSlotsPerDay;

    /** Break windows configured for this day. Empty list means no breaks. */
    private List<BreakWindowResponse> breakWindows;
}
