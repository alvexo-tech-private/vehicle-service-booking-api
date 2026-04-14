package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VehicleUserRegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid mobile number")
    private String mobileNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "City is required")
    private String city;

    // Optional
    private String area;

    @NotNull(message = "PIN is required")
    @Min(value = 1000, message = "PIN must be 4 digits")
    @Max(value = 9999, message = "PIN must be 4 digits")
    private Integer pin;
   
    @NotNull(message = "Confirm PIN is required")
    @Min(value = 1000, message = "PIN must be 4 digits")
    @Max(value = 9999, message = "PIN must be 4 digits")
    private Integer confirmPin;
}
