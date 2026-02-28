package com.alvexo.bookingapp.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.UserUpdateRequest;
import com.alvexo.bookingapp.dto.response.ApiResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getMobileNumber() != null) user.setMobileNumber(request.getMobileNumber());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getAddressLine1() != null) user.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) user.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getLatitude() != null) user.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) user.setLongitude(request.getLongitude());
        if (request.getBio() != null) user.setBio(request.getBio());
        
        user = userRepository.save(user);
        
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    private UserResponse convertToResponse(User user) {
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
                .build();
    }
}
