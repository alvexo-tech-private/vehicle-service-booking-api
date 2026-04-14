package com.alvexo.bookingapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.alvexo.bookingapp.dto.request.ChangePinRequest;
import com.alvexo.bookingapp.dto.request.UserUpdateRequest;
import com.alvexo.bookingapp.dto.response.MyApiResponse;
import com.alvexo.bookingapp.dto.response.UserResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.service.AuthService;

@Tag(name = "Users", description = "Profile management and PIN change for all authenticated users. Requires JWT token.")
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    @Operation(summary = "Get my profile", description = "Returns the full profile of the currently authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<MyApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(MyApiResponse.success(response));
    }
    
    @Operation(summary = "Update my profile", description = "Updates editable profile fields (name, address, bio, etc). Only provided fields are changed.")

    @PutMapping("/me")
    public ResponseEntity<MyApiResponse<UserResponse>> updateProfile(
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
        return ResponseEntity.ok(MyApiResponse.success("Profile updated successfully", response));
    }

    // -------------------------------------------------------------------------
    // PIN management — works for all 4 user types
    // -------------------------------------------------------------------------

    /**
     * PATCH /api/users/me/change-pin
     *
     * Changes the 4-digit PIN for the currently authenticated user.
     * Works for ALL roles: VEHICLE_USER, MECHANIC, SALES_REPRESENTATIVE, ADMINISTRATOR.
     *
     * - currentPin must match the stored PIN.
     * - newPin and confirmPin must match.
     * - newPin must differ from currentPin.
     * - All existing refresh tokens are invalidated — other devices must re-login.
     */
    @Operation(summary = "Change PIN", description = "Changes the 4-digit PIN. Requires current PIN for verification. Invalidates all refresh tokens — forces re-login on all devices. Works for all 4 user roles.")
    @PatchMapping("/me/change-pin")
    public ResponseEntity<MyApiResponse<Void>> changePin(
            @Valid @RequestBody ChangePinRequest request,
            Authentication authentication) {
        authService.changePin(authentication.getName(), request);
        return ResponseEntity.ok(
                MyApiResponse.success("PIN changed successfully. Please log in again on all devices.", null));
    }

    @Operation(summary = "Get user by ID", description = "Returns the public profile of any user by their database ID.")
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserResponse response = convertToResponse(user);
        return ResponseEntity.ok(MyApiResponse.success(response));
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
