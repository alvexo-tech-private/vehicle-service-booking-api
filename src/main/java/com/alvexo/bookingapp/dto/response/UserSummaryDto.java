package com.alvexo.bookingapp.dto.response;

import lombok.Data;

@Data
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
    // No refreshTokens field!
}