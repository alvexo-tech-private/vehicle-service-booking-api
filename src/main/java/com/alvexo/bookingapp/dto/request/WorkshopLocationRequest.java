package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkshopLocationRequest {

    @NotBlank(message = "addressLine1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "city is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "state is required")
    private String state;

    @NotBlank(message = "pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "pincode must be a valid 6-digit Indian PIN code")
    private String pincode;

    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
    private BigDecimal longitude;
}
