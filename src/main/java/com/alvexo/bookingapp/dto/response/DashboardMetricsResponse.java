package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardType;
import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real-time dashboard metrics for mechanic home screen — booking counts, capacity, slot utilization")
public class DashboardMetricsResponse {

    @Schema(description = "Dashboard date", example = "2026-07-15")
    private LocalDate date;

    @Schema(description = "Mechanic's job card type", example = "TYPE_3")
    private JobCardType jobCardType;

    @Schema(description = "Effective capacity in hours for today", example = "36.00")
    private BigDecimal effectiveCapacityHours;

    @Schema(description = "Total active bookings for today", example = "19")
    private Long totalBookedCount;

    @Schema(description = "Total booked hours for today", example = "28.50")
    private BigDecimal totalBookedHours;

    @Schema(description = "Remaining capacity in hours", example = "7.50")
    private BigDecimal remainingCapacityHours;

    @Schema(description = "Bookings auto-confirmed by the system", example = "8")
    private Long autoConfirmedCount;

    @Schema(description = "Bookings pending mechanic review", example = "4")
    private Long pendingReviewCount;

    @Schema(description = "Bookings created via rider app", example = "12")
    private Long riderAppCount;

    @Schema(description = "Walk-in bookings created by mechanic", example = "7")
    private Long walkInCount;

    @Schema(description = "Whether a daily quota override is active for today", example = "false")
    private Boolean quotaOverrideActive;

    @Schema(description = "Workshop technician capacity summary")
    private TechnicianSummary technicianSummary;

    @Schema(description = "TYPE_4 slot utilization details")
    private List<SlotUtilization> slotUtilization;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Aggregated technician capacity summary")
    public static class TechnicianSummary {

        @Schema(description = "Total registered technicians", example = "4")
        private Long totalTechnicians;

        @Schema(description = "Currently active (present) technicians", example = "4")
        private Long activeTechnicians;

        @Schema(description = "SUM of active technicians' reserved hours", example = "36.00")
        private BigDecimal totalReservedHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual slot booking utilization for today")
    public static class SlotUtilization {

        @Schema(description = "Slot ID", example = "10")
        private Long slotId;

        @Schema(description = "Service category", example = "REPAIR")
        private ServiceCategory restrictedCategory;

        @Schema(description = "Slot start time", example = "11:00")
        private LocalTime startTime;

        @Schema(description = "Slot end time", example = "13:00")
        private LocalTime endTime;

        @Schema(description = "Total slot capacity", example = "5")
        private Integer maxVehicleQty;

        @Schema(description = "Total bookings in this slot today", example = "3")
        private Long bookedCount;

        @Schema(description = "Auto-confirmed bookings in slot", example = "2")
        private Long autoConfirmedCount;

        @Schema(description = "Pending review bookings in slot", example = "1")
        private Long pendingReviewCount;

        @Schema(description = "Active weekdays", example = "MON,WED,FRI")
        private String applicableDays;
    }
}
