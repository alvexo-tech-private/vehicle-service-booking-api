package com.alvexo.bookingapp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVehicleResponseDto {

    private Long id;
    private Boolean isPrimary;

    // User summary — no sensitive fields like refreshTokens
    private Long userId;
    private String userEmail;
    private String userName;

    // Vehicle summary
    private Long vehicleId;
    private String vehicleName;
    private String vehicleNumber;
    private String vehicleType;
    private String licensePlate;
    private String vehicleImage;
    private String insuranceNumber;

}