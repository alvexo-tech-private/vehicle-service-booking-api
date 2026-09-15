package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingPayResponse {
    private Boolean feePaid;
    private String paymentRef;
}
