package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<UserVehicleResponseDto> userVehicleResponseDtos;

    public static UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .mobileVerified(user.getMobileVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .city(user.getCity())
                .state(user.getState())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .specialization(user.getSpecialization())
                .experienceYears(user.getExperienceYears())
                .hourlyRate(user.getHourlyRate())
                .bio(user.getBio())
                .rating(user.getRating())
                .totalReviews(user.getTotalReviews())
                .totalBookingsCompleted(user.getTotalBookingsCompleted())
                .referralCode(user.getReferralCode())
                .totalReferrals(user.getTotalReferrals())
                .totalBonusEarned(user.getTotalBonusEarned())
                .createdAt(user.getCreatedAt())
                .userVehicleResponseDtos(user.getUserVehicles() != null
                        ? user.getUserVehicles().stream()
                        .map(UserVehicleResponseDto::from)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
