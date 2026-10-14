package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.WeekStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service Week tab (§2.1) list item. Service Desk has no separate storage —
 * {@code id} and {@code bookingId} are both the underlying booking's
 * bookingNumber; {@code id} is kept as a distinct field to match the client
 * contract (used as the reschedule/cancel path key).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekVehicleResponse {
    private String id;
    private String bookingId;
    private String ownerName;
    private String mobile;
    private String registrationNumber;
    private String make;
    private String model;
    private String regnLast4;
    private String serviceType;
    private Boolean pickupRequired;
    private String reportingTime;
    private String customerRemarks;
    private WeekStatus status;
    private String cancellationMessage;
}
