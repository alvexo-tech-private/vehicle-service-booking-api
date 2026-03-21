package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

import com.alvexo.bookingapp.model.UserRole;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Boolean active;
    private LocalDateTime createdAt;

    // No refreshTokens field!
}