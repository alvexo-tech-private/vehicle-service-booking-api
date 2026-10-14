package com.alvexo.bookingapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "currentPassword is required")
    private String currentPassword;

    @NotBlank(message = "newPassword is required")
    @Size(min = 8, max = 100, message = "newPassword must be at least 8 characters")
    @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "newPassword must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String newPassword;
}
