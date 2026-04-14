package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleRequestDTO {
	
    @NotNull(message = "Vehicle ID is Required")
    private Long vehicleId;
    
    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2100, message = "Year must be before 2100")
    private Integer registraionYear;
    
    private Boolean isPrimary;
    
    private String registrationNumber;
    
    private String color;
    private Integer mileage;
    private String engineType;
    private String imageUrl;
}
