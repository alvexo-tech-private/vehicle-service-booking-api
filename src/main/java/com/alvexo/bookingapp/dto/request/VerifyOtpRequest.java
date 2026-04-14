package com.alvexo.bookingapp.dto.request;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String mobileNumber;
    private String otp;

    private String deviceId;
    private String deviceType;
}
