package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Workshop mechanic registration — creates a MECHANIC account with workshop details")
public class WorkshopMechanicRegisterRequest {

    @Schema(description = "Full name", example = "Suresh Kumar", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "10-digit mobile number", example = "9876543210",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid mobile number")
    private String mobileNumber;

    @Schema(description = "Email address", example = "suresh@workshop.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "City name", example = "Chennai", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "City is required")
    private String city;

    @Schema(description = "Area within city", example = "Anna Nagar", nullable = true)
    private String area;

    @Schema(description = "4-digit login PIN", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PIN is required")
    @Min(value = 1000, message = "PIN must be 4 digits")
    @Max(value = 9999, message = "PIN must be 4 digits")
    private Integer pin;

    @Schema(description = "Confirm PIN — must match pin", example = "1234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Confirm PIN is required")
    @Min(value = 1000, message = "PIN must be 4 digits")
    @Max(value = 9999, message = "PIN must be 4 digits")
    private Integer confirmPin;

    @Schema(description = "Workshop name", example = "Suresh Auto Care",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Workshop name is required")
    private String workshopName;

    @Schema(description = "6-digit postal/pin code for search", example = "600040",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "postalCode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "postalCode must be 6 digits")
    private String postalCode;
}
