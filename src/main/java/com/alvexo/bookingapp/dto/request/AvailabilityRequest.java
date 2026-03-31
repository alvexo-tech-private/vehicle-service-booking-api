package com.alvexo.bookingapp.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.alvexo.bookingapp.model.DayOfWeek;

@Schema(description = "Availability template for one day of the week")
@Data
public class AvailabilityRequest {

    @NotNull(message = "Day of week is required")
    @Schema(example = "MONDAY", description = "MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    @Schema(example = "09:00")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    @Schema(example = "18:00")
    private LocalTime endTime;

    @Schema(example = "true", description = "Set false to mark the mechanic as unavailable on this day")
    private Boolean isAvailable;

    @Min(value = 15, message = "Slot duration must be at least 15 minutes")
    @Schema(example = "60", description = "Duration of each bookable slot in minutes. Default 60.")
    private Integer slotDurationMinutes;

    @Min(value = 1, message = "Max slots per day must be at least 1")
    @Schema(example = "8", description = "Maximum bookings allowed on this day. Default 10.")
    private Integer maxSlotsPerDay;

    /**
     * Optional break windows. Slots overlapping a break are excluded from slot discovery.
     * Leave empty or omit for no breaks.
     *
     * Example: lunch 13:00-14:00, tea 16:00-16:15
     */
    @Valid
    @Schema(description = "Break windows within the working day. Slots inside breaks are hidden from vehicle users.")
    private List<BreakWindowRequest> breakWindows = new ArrayList<>();
}
