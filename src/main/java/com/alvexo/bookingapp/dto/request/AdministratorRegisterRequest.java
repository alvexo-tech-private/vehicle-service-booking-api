package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for admin-only endpoint:
 * POST /api/admin/users/administrator
 *
 * Only an existing ADMINISTRATOR can create another administrator.
 */
@Data
public class AdministratorRegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^\\+?0?[0-9]{10,15}$",
        message = "Invalid mobile number — accepted formats: 9876543210 / +919876543210 / 919876543210"
    )
    private String mobileNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "City is required")
    private String city;

    // Optional
    private String area;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
