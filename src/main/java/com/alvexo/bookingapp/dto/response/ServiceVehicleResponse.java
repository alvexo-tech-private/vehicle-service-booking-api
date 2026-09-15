package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Today tab (§1.1) list item — one row per vehicle in the carry-over/today/cancelled partitions. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVehicleResponse {
    private String bookingId;
    private String jobCardNumber;
    private String make;
    private String model;
    private String regnLast4;
    private String serviceType;
    private ServiceWorkspaceStatus status;
    private Boolean isCarryOver;
    private String cancellationMessage;
}
