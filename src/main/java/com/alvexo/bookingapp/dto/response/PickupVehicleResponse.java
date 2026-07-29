package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pickup tab (§3.1) list item. Assignment status is derived: assignedMechanicId != null → Assigned. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVehicleResponse {
    private String bookingId;
    private String ownerName;
    private String mobile;
    private String vehicleNumber;
    private String pickupAddress;
    private Long assignedMechanicId;
    private String assignedMechanicName;
}
