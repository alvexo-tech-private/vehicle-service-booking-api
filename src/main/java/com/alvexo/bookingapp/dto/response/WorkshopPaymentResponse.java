package com.alvexo.bookingapp.dto.response;

import com.alvexo.bookingapp.model.PaymentEligibility;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopPaymentResponse {
    private PaymentEligibility eligibility;
    private List<StepStatus> statuses;
    private String partner;
    private String accountRef;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepStatus {
        private String key;
        private String label;
        private String state;
    }
}
