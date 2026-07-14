package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** GET /api/service-desk/bookings/{bookingId} (§1.2) — Selected Vehicle panel. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingInfoResponse {
    private String bookingId;
    private String ownerName;
    private String mobile;
    private String registrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private String serviceType;
    private Boolean pickupRequired;
    private Boolean dropRequired;
    private String reportingTime;
    private String customerRemarks;
    private BigDecimal advancePaid;
}
