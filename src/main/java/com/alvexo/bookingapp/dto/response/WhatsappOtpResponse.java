package com.alvexo.bookingapp.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappOtpResponse {
    private Boolean otpSent;
    private Boolean verified;
    private Integer expiresInSec;
}
