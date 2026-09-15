package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPhoneResponse {
    private boolean exists;
    private UserRole role;
    private Boolean isVerified;
    private String message;
}
