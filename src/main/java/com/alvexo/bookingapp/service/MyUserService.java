package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class MyUserService {

    @Autowired
    private UserVehicleService userVehicleService;

    public UserResponse convertToResponse(User user) {
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
                        .map(userVehicleService::from)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

}
