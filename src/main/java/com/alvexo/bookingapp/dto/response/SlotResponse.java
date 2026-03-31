package com.alvexo.bookingapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a single bookable time slot for a mechanic on a specific date.
 * Computed on-demand — not read from pre-generated rows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A bookable time slot for a mechanic on a specific date")
public class SlotResponse {

    @Schema(description = "Date of the slot", example = "2025-07-15")
    private LocalDate slotDate;

    @Schema(description = "Slot start time", example = "09:00")
    private LocalTime slotStart;

    @Schema(description = "Slot end time", example = "10:00")
    private LocalTime slotEnd;

    @Schema(description = "Full scheduled date-time — pass this as scheduledDateTime when creating a booking", example = "2025-07-15T09:00:00")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "Mechanic's database ID", example = "5")
    private Long mechanicId;

    @Schema(description = "Mechanic's full name", example = "Suresh Mech")
    private String mechanicName;

    @Schema(description = "Slot duration in minutes", example = "60")
    private Integer slotDurationMinutes;
}
