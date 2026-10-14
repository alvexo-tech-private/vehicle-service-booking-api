package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalTrustResponse {
    private String outcome; // under_evaluation | verified | suspended
    private Boolean suspended;
    private String reason;
}
