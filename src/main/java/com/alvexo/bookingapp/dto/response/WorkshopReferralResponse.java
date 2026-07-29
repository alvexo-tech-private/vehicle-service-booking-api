package com.alvexo.bookingapp.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopReferralResponse {
    private String code;
    private String partnerName;
    private String benefit;
    private LocalDateTime appliedAt;
}
