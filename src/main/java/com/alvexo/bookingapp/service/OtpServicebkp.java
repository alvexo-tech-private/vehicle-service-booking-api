package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.alvexo.bookingapp.model.OtpVerification;
import com.alvexo.bookingapp.repository.OtpVerificationRepository;
import com.alvexo.bookingapp.util.OtpUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServicebkp {

    private final OtpVerificationRepository otpRepository;

    public String generateAndSaveOtp(String mobileNumber) {

        String otp = OtpUtil.generateOtp();

        OtpVerification entity = OtpVerification.builder()
                .mobileNumber(mobileNumber)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(entity);

        return otp;
    }

    public boolean validateOtp(String mobileNumber, String otp) {

        OtpVerification record = otpRepository
                .findTopByMobileNumberOrderByCreatedAtDesc(mobileNumber)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!record.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        record.setVerified(true);
        otpRepository.save(record);

        return true;
    }
}
