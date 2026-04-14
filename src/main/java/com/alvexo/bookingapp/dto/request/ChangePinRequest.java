package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for PUT /api/users/me/change-pin
 *
 * Used by all 4 user types (VEHICLE_USER, MECHANIC, SALES_REPRESENTATIVE, ADMINISTRATOR).
 * The current PIN is verified before the new PIN is saved.
 */
@Data
public class ChangePinRequest {

    @NotBlank(message = "Current PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Current PIN must be exactly 4 digits")
    private String currentPin;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "New PIN must be exactly 4 digits")
    private String newPin;

    @NotBlank(message = "Confirm PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Confirm PIN must be exactly 4 digits")
    private String confirmPin;
}
