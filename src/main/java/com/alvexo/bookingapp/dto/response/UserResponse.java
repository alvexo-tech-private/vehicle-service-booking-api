package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile details — fields populated vary by role")
public class UserResponse {

    @Schema(description = "User ID", example = "5")
    private Long id;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Mobile number", example = "9876543210")
    private String mobileNumber;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "User role", example = "MECHANIC")
    private UserRole role;

    @Schema(description = "Account active status", example = "true")
    private Boolean active;

    @Schema(description = "Email verified", example = "false")
    private Boolean emailVerified;

    @Schema(description = "Mobile verified", example = "true")
    private Boolean mobileVerified;

    @Schema(description = "Profile image URL", nullable = true)
    private String profileImageUrl;

    @Schema(description = "Address line 1", nullable = true)
    private String addressLine1;

    @Schema(description = "Address line 2", nullable = true)
    private String addressLine2;

    @Schema(description = "City", example = "Chennai")
    private String city;

    @Schema(description = "State", example = "Tamil Nadu")
    private String state;

    @Schema(description = "Postal/pin code", example = "600040")
    private String postalCode;

    @Schema(description = "Country", example = "India")
    private String country;

    @Schema(description = "Latitude for location search", example = "13.08268700")
    private BigDecimal latitude;

    @Schema(description = "Longitude for location search", example = "80.27071800")
    private BigDecimal longitude;

    @Schema(description = "Mechanic specialization", example = "Bike, Scooter", nullable = true)
    private String specialization;

    @Schema(description = "Years of experience", example = "5", nullable = true)
    private Integer experienceYears;

    @Schema(description = "Hourly rate in INR", example = "250.00", nullable = true)
    private BigDecimal hourlyRate;

    @Schema(description = "Mechanic bio", nullable = true)
    private String bio;

    @Schema(description = "Average rating (0-5)", example = "4.50", nullable = true)
    private BigDecimal rating;

    @Schema(description = "Total reviews received", example = "42")
    private Integer totalReviews;

    @Schema(description = "Total bookings completed", example = "150")
    private Integer totalBookingsCompleted;

    @Schema(description = "Referral code (SALES_REPRESENTATIVE only)", nullable = true)
    private String referralCode;

    @Schema(description = "Total referrals made", example = "10", nullable = true)
    private Integer totalReferrals;

    @Schema(description = "Total bonus earned from referrals", example = "5000.00", nullable = true)
    private BigDecimal totalBonusEarned;

    private LocalDateTime createdAt;
}
