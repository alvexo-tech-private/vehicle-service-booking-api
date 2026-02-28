package com.alvexo.bookingapp.dto.request;

import com.alvexo.bookingapp.model.VehicleType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequest {
    @NotBlank(message = "Make is required")
    private String manufacturer;
    
    @NotBlank(message = "Model is required")
    private String modelName;
    
    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2100, message = "Year must be before 2100")
    private Integer year;
    
    private String vin;
    private String licensePlate;
    
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    
    private String color;
    private Integer mileage;
    private String engineType;
    private String transmissionType;
    private String fuelType;
    private String imageUrl;
}
