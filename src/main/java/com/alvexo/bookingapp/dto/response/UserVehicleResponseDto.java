package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.UserVehicle;
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
    private String vehicleType;
    private String licensePlate;

    public static UserVehicleResponseDto from(UserVehicle uv) {
        return UserVehicleResponseDto.builder()
                .id(uv.getId())
                .isPrimary(uv.getIsPrimary())
                // User fields
                .userId(uv.getUser().getId())
                .userEmail(uv.getUser().getEmail())
                .userName(uv.getUser().getFirstName() + uv.getUser().getLastName())
                // Vehicle fields — adjust getters to match your Vehicle model
                .vehicleId(uv.getVehicle().getId())
                .vehicleName(uv.getVehicle().getMake())
                .vehicleType(uv.getVehicle().getVehicleType().name())
                .licensePlate(uv.getVehicle().getLicensePlate())
                .build();
    }
}