package com.alvexo.bookingapp.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.ServiceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull(message = "Mechanic ID is required")
    private Long mechanicId;
    
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;
    
    @NotNull(message = "Scheduled date time is required")
    private LocalDateTime scheduledDateTime;
    
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
    
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * FK → mechanic_service_settings.id
     * Required when the mechanic operates in hour-slot mode (reserveCapacity = true).
     * Optional in vehicle-count mode — can still be provided for reference.
     */
    private Long serviceSettingId;

    /**
     * Advance amount paid by the customer at booking time.
     * Must match mechanic_settings.advance_amount when advanceEnabled = true.
     */
    private BigDecimal advancePaid;

    private BigDecimal estimatedCost;
    private Integer estimatedDurationMinutes;
    private String customerNotes;

    /**
     * Pickup/drop request (RIDER_BOOKING_TO_WORKSHOP.md §3.2), folded into the
     * booking create call rather than a separate service-details endpoint so
     * the two can never race or be left unlinked.
     */
    private Boolean pickupRequired;
    private String pickupAddress;
    private Boolean dropRequired;
    private String deliveryAddress;

    /** Reference to a rider-recorded voice note, uploaded out-of-band (e.g. a StoredFile id/filename). */
    @Size(max = 255, message = "audioReference must be at most 255 characters")
    private String audioReference;

    private Boolean engineOilReplacement;

    /**
     * Client-generated key (e.g. a UUID) that de-duplicates a submission retried after a
     * timeout/dropped-response. Replaying the same key returns the original booking instead
     * of creating a second one (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §4).
     */
    @Size(max = 100, message = "idempotencyKey must be at most 100 characters")
    private String idempotencyKey;

    /**
     * When true, submits a Today Approval request (status REQUESTED) instead of an instant
     * booking — the workshop must accept/reject, and the rider then confirms
     * (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §5). No capacity check runs at this
     * stage and no advance is charged.
     */
    private Boolean todayApprovalRequest;
}
