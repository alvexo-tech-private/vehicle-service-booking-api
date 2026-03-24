package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserVehicleRequest {
    @NotNull(message = "Vehicle Id must not be empty")
    private Long vehicleId;
    @NotBlank(message = "Vehicle Number must not be empty")
    private String vehicleNumber;
    private String vehicleImage;
    private String insuranceNumber;
    private Boolean isPrimary;
}
