package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT authentication tokens returned after login or registration")
public class TokenResponse {

    @Schema(description = "JWT access token for API calls", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh token for obtaining new access tokens", example = "a1b2c3d4-e5f6-7890...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token TTL in milliseconds", example = "3600000")
    private Long expiresIn;

    @Schema(description = "Authenticated user ID", example = "5")
    private Long userId;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "User role", example = "VEHICLE_USER")
    private UserRole role;

    @Schema(description = "Referral code (SALES_REPRESENTATIVE only)", example = "REFABC12345",
            nullable = true)
    private String referralCode;
}
