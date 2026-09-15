package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Drop tab (§4.1) list item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropVehicleResponse {
    private String bookingId;
    private String jobCardNumber;
    private String ownerName;
    private String mobile;
    private String vehicleNumber;
    private String deliveryAddress;
    private Long assignedMechanicId;
    private String assignedMechanicName;
    private String deliveredBy;
    private LocalDate deliveredOn;
    private AssignmentStatus assignmentStatus;
}
