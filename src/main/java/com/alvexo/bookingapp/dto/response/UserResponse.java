package com.alvexo.bookingapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Boolean active;
    private Boolean emailVerified;
    private Boolean mobileVerified;
    private String profileImageUrl;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String specialization;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private String bio;
    private BigDecimal rating;
    private Integer totalReviews;
    private Integer totalBookingsCompleted;
    private String referralCode;
    private Integer totalReferrals;
    private BigDecimal totalBonusEarned;
    private LocalDateTime createdAt;
}
