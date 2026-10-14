package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralValidateResponse {
    private Boolean valid;
    private String partnerName;
    private String benefit;
}
