package com.alvexo.bookingapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Login with email or mobile number + 4-digit PIN")
public class LoginRequest {

    @Schema(description = "Email address or mobile number", example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username (email or mobile) is required")
    private String username;

    @Schema(description = "4-digit PIN", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Password is required")
    private Integer password;

    @Schema(description = "Device identifier for token management", example = "device-abc-123",
            nullable = true)
    private String deviceId;

    @Schema(description = "Device type", example = "ANDROID", nullable = true)
    private String deviceType;
}
