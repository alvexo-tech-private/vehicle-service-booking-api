package com.alvexo.bookingapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * A break window within a mechanic's working day.
 *
 * Stored as a JSONB array in mechanic_availability.break_windows.
 * No separate table — breaks are config that belongs with the availability rule.
 *
 * DB value example:
 *   [{"start":"13:00","end":"14:00","label":"Lunch"},
 *    {"start":"16:00","end":"16:15","label":"Tea break"}]
 *
 * During slot computation any slot that overlaps a break is silently excluded —
 * the vehicle user never sees break-time slots in GET /{id}/slots.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakWindow implements Serializable {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime start;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime end;

    /** Human-readable label, e.g. "Lunch", "Prayer break". Optional. */
    private String label;

    /**
     * Returns true if the given slot [slotStart, slotEnd) overlaps this break.
     * Overlap condition: slot starts before break ends AND slot ends after break starts.
     */
    public boolean overlaps(LocalTime slotStart, LocalTime slotEnd) {
        return slotStart.isBefore(end) && slotEnd.isAfter(start);
    }
}
