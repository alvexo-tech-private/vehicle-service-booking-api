package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.AllocationResult;
import com.alvexo.bookingapp.model.BookingSource;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.BookingType;
import com.alvexo.bookingapp.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking details including allocation result and walk-in metadata")
public class BookingResponse {

    @Schema(description = "Booking ID", example = "123")
    private Long id;

    @Schema(description = "Unique booking reference number", example = "BKG1653209000011ABC")
    private String bookingNumber;

    @Schema(description = "Job card number, generated on confirmation", example = "010126062103")
    private String jobCardNumber;

    @Schema(description = "Vehicle user ID (null for unregistered walk-ins)", example = "10")
    private Long vehicleUserId;

    @Schema(description = "Vehicle user name", example = "John Doe")
    private String vehicleUserName;

    @Schema(description = "Mechanic user ID", example = "5")
    private Long mechanicId;

    @Schema(description = "Mechanic name", example = "Suresh Mech")
    private String mechanicName;

    @Schema(description = "Vehicle ID", example = "15")
    private Long vehicleId;

    @Schema(description = "Vehicle make, model, year", example = "Honda Activa 2022")
    private String vehicleInfo;

    @Schema(description = "Linked mechanic service setting ID", example = "10")
    private Long serviceSettingId;

    @Schema(description = "Service setting name", example = "Oil Change")
    private String serviceSettingName;

    @Schema(description = "Scheduled service date and time", example = "2026-07-15T09:00:00")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "Current booking status", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "STANDARD or EXPRESS", example = "STANDARD")
    private BookingType bookingType;

    @Schema(description = "Service type category", example = "REGULAR_MAINTENANCE")
    private ServiceType serviceType;

    @Schema(description = "Service description")
    private String description;

    @Schema(description = "Estimated cost in INR", example = "500.00")
    private BigDecimal estimatedCost;

    @Schema(description = "Actual cost after completion", example = "450.00")
    private BigDecimal actualCost;

    @Schema(description = "Estimated duration in minutes", example = "60")
    private Integer estimatedDurationMinutes;

    @Schema(description = "Actual duration after completion", example = "55")
    private Integer actualDurationMinutes;

    @Schema(description = "Advance amount paid", example = "100.00")
    private BigDecimal advancePaid;

    @Schema(description = "Who created the booking: RIDER_APP or WALK_IN", example = "RIDER_APP")
    private BookingSource bookingSource;

    @Schema(description = "How the booking was processed: AUTO_CONFIRMED or MANUAL_REVIEW", example = "AUTO_CONFIRMED")
    private AllocationResult allocationResult;

    @Schema(description = "Service slot ID for TYPE_4 slot bookings", example = "10")
    private Long serviceSlotId;

    @Schema(description = "Walk-in customer name (unregistered customers)", example = "Ravi Kumar")
    private String walkInCustomerName;

    @Schema(description = "Walk-in customer mobile", example = "9876543210")
    private String walkInCustomerMobile;

    @Schema(description = "Walk-in vehicle description", example = "Honda Activa 2022 - TN09AB1234")
    private String walkInVehicleDescription;

    @Schema(description = "Notes from the mechanic")
    private String mechanicNotes;

    @Schema(description = "Notes from the customer")
    private String customerNotes;

    @Schema(description = "Reason for cancellation")
    private String cancellationReason;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
