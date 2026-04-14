package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Sets or replaces the mechanic's availability for the entire week in one call.
 *
 * Rules:
 *  - You can include 1 to 7 day entries. Days not included are left unchanged.
 *  - If you want to replace the full week, include all 7 days.
 *  - Set isAvailable=false on any day to mark it as a day off.
 *  - Each day can carry its own break windows.
 *
 * Example — full week with lunch break Mon-Fri and half day Saturday:
 * {
 *   "days": [
 *     { "dayOfWeek":"MONDAY",    "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
 *       "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
 *     { "dayOfWeek":"TUESDAY",   "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
 *       "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
 *     { "dayOfWeek":"WEDNESDAY", "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
 *       "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
 *     { "dayOfWeek":"THURSDAY",  "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
 *       "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
 *     { "dayOfWeek":"FRIDAY",    "startTime":"09:00","endTime":"18:00","slotDurationMinutes":60,
 *       "breakWindows":[{"start":"13:00","end":"14:00","label":"Lunch"}] },
 *     { "dayOfWeek":"SATURDAY",  "startTime":"09:00","endTime":"13:00","slotDurationMinutes":60 },
 *     { "dayOfWeek":"SUNDAY",    "startTime":"09:00","endTime":"18:00","isAvailable":false }
 *   ]
 * }
 */
@Schema(description = "Sets the mechanic's availability for multiple days in one request")
@Data
public class WeeklyAvailabilityRequest {

    @NotEmpty(message = "At least one day must be provided")
    @Valid
    @Schema(description = "List of day availability configs. Include 1-7 days.")
    private List<AvailabilityRequest> days;
}
