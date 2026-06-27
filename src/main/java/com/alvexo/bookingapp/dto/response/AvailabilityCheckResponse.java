package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.JobCardType;
import com.alvexo.bookingapp.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Per-category booking availability for a mechanic on a date. Used by rider UI for grey-out logic")
public class AvailabilityCheckResponse {

    @Schema(description = "Mechanic ID", example = "5")
    private Long mechanicId;

    @Schema(description = "Queried date", example = "2026-07-15")
    private LocalDate date;

    @Schema(description = "Mechanic's job card type", example = "TYPE_2")
    private JobCardType jobCardType;

    @Schema(description = "Effective daily capacity in hours", example = "36.00")
    private BigDecimal effectiveCapacity;

    @Schema(description = "Hours consumed by active bookings", example = "28.00")
    private BigDecimal usedCapacity;

    @Schema(description = "true if no capacity remains for any category", example = "false")
    private Boolean isFullyBooked;

    @Schema(description = "Available booking count per service category. 0 = grey out this category for this date")
    private Map<ServiceCategory, Integer> availableByCategory;

    @Schema(description = "TYPE_4 slot availability details")
    private List<SlotAvailability> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual slot availability for a date")
    public static class SlotAvailability {

        @Schema(description = "Slot ID", example = "10")
        private Long slotId;

        @Schema(description = "Slot start time", example = "11:00")
        private LocalTime startTime;

        @Schema(description = "Slot end time", example = "13:00")
        private LocalTime endTime;

        @Schema(description = "Service category this slot serves", example = "REPAIR")
        private ServiceCategory restrictedCategory;

        @Schema(description = "Total vehicle capacity", example = "5")
        private Integer maxVehicleQty;

        @Schema(description = "Vehicles already booked in this slot", example = "3")
        private Long bookedCount;

        @Schema(description = "false if slot is full or not applicable on this day", example = "true")
        private Boolean isAvailable;

        @Schema(description = "Active weekdays", example = "MON,WED,FRI")
        private String applicableDays;
    }
}
