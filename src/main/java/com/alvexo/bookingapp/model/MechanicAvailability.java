package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Weekly availability template for a mechanic.
 * Max 7 rows per mechanic (one per day of week).
 *
 * Break windows are stored as JSONB in break_windows column — no separate table.
 * Slots that fall inside any break window are automatically excluded during
 * on-demand slot computation.
 */
@Entity
@Table(name = "mechanic_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** Working day start time, e.g. 09:00 */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    /** Duration of each bookable slot in minutes. Default 60. */
    @Column(name = "slot_duration_minutes")
    @Builder.Default
    private Integer slotDurationMinutes = 60;

    @Column(name = "max_slots_per_day")
    @Builder.Default
    private Integer maxSlotsPerDay = 10;

    /**
     * Break windows stored as JSONB — no join, no separate table.
     *
     * Example value in DB:
     *   [{"start":"13:00","end":"14:00","label":"Lunch"},
     *    {"start":"16:00","end":"16:15","label":"Tea break"}]
     *
     * Any slot whose time window overlaps a break is excluded during
     * slot computation. Breaks are invisible to vehicle users.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "break_windows", columnDefinition = "jsonb")
    @Builder.Default
    private List<BreakWindow> breakWindows = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Domain logic — called during on-demand slot computation
    // -------------------------------------------------------------------------

    /**
     * Returns true if the slot [slotStart, slotEnd) overlaps any break window.
     */
    public boolean isInBreak(LocalTime slotStart, LocalTime slotEnd) {
        if (breakWindows == null || breakWindows.isEmpty()) return false;
        return breakWindows.stream().anyMatch(b -> b.overlaps(slotStart, slotEnd));
    }

    /**
     * Computes all bookable slot start times from this template.
     * Excludes: already-booked times, break windows, and past slots (handled by caller).
     *
     * @param bookedStartTimes LocalTime set of already-booked slots on the target date
     */
    public List<LocalTime> computeAvailableSlotStarts(Set<LocalTime> bookedStartTimes) {
        List<LocalTime> result = new ArrayList<>();
        LocalTime cursor = startTime;
        int duration = slotDurationMinutes != null ? slotDurationMinutes : 60;

        while (!cursor.plusMinutes(duration).isAfter(endTime)) {
            LocalTime slotEnd = cursor.plusMinutes(duration);

            boolean booked   = bookedStartTimes.contains(cursor);
            boolean inBreak  = isInBreak(cursor, slotEnd);

            if (!booked && !inBreak) {
                result.add(cursor);
                if (result.size() + bookedStartTimes.size() >= maxSlotsPerDay) break;
            }
            cursor = slotEnd;
        }
        return result;
    }
}
