package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String make;
    private String model;
    private Integer year;
    private String vin;
    private VehicleType vehicleType;
    private String color;
    private FuelType fuelType;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;
}
