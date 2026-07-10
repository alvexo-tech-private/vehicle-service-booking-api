package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardType;
import com.alvexo.bookingapp.model.WorkshopClassification;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/mechanic-dashboard/today response.
 * Fields are populated regardless of classification; the client renders only
 * the tiles relevant to its Level (see WORKSHOP_SETTINGS_HOME_SPEC.md §3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MechanicDashboardResponse {

    private LocalDate date;
    private JobCardType jobCardType;
    private WorkshopClassification classification;

    /** Type 1 "MAX VEHICLES" card. */
    private Integer maxVehiclesPerDay;

    /** CONFIRMED — total active bookings today (excludes cancelled/rejected). */
    private Long totalBookedCount;

    /** JC ISSUED — bookings today that already have a job card number. */
    private Long autoConfirmedCount;

    /** Job Card Waiting List — bookings today still PENDING (Types 3/4). */
    private Long pendingCount;

    /** Bookings today recorded via BookingChannel.WALK_IN. */
    private Long walkInCount;

    /** Bookings today recorded via BookingChannel.RIDER_APP. */
    private Long riderCount;

    /** Percentage of the day's capacity consumed (vehicle-count or hour-slot basis). */
    private BigDecimal capacityUtilizationPercent;

    /** Service table F/I/P rows (Types 2/3/4). */
    private List<ServiceBreakdownResponse> serviceBreakdown;

    /** Slot 1/2 cards (Type 4 only) — empty list otherwise. */
    private List<ServiceSlotSummaryResponse> slots;
}
