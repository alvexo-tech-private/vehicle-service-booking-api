package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.OtpVerification;
import com.alvexo.bookingapp.repository.OtpVerificationRepository;

@Service
public class OtpService {
    
    @Autowired
    private OtpVerificationRepository otpRepository;
    
    @Autowired(required = false)
    private SmsService smsService; // Optional SMS service
    
    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;
    
    @Value("${otp.max.attempts.per.hour:3}")
    private int maxOtpAttemptsPerHour;
    
    private static final int OTP_LENGTH = 6;
    private static final Random random = new Random();
    
    /**
     * Generate and send OTP to mobile number
     */
    @Transactional
    public OtpVerification generateAndSendOtp(String mobileNumber) {
        // Validate mobile number format
        if (!isValidMobileNumber(mobileNumber)) {
            throw new BadRequestException("Invalid mobile number format");
        }
        
        // Check rate limiting (prevent spam)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentAttempts = otpRepository.countByMobileNumberAndVerifiedFalseAndCreatedAtAfter(
                mobileNumber, oneHourAgo);
        
        if (recentAttempts >= maxOtpAttemptsPerHour) {
            throw new BadRequestException(
                    "Too many OTP requests. Please try again after some time.");
        }
        
        // Generate OTP
        String otp = generateOtp();
        
        // Calculate expiry time
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        
        // Save OTP to database
        OtpVerification otpVerification = OtpVerification.builder()
                .mobileNumber(mobileNumber)
                .otp(otp)
                .expiryTime(expiryTime)
                .verified(false)
                .build();
        
        otpVerification = otpRepository.save(otpVerification);
        
        // Send OTP via SMS
       // sendOtpViaSms(mobileNumber, otp);
        
        // Don't return the actual OTP in production
        return otpVerification;
    }
    
    /**
     * Verify OTP for a mobile number
     */
    @Transactional
    public boolean verifyOtp(String mobileNumber, String otp) {
        // Find the latest valid OTP
        Optional<OtpVerification> otpOpt = otpRepository.findLatestValidOtp(
                mobileNumber, LocalDateTime.now());
        
        if (otpOpt.isEmpty()) {
            throw new BadRequestException("OTP not found or expired");
        }
        
        OtpVerification otpVerification = otpOpt.get();
        
        // Check if OTP matches
        if (!otpVerification.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        
        // Mark as verified
        otpVerification.setVerified(true);
        otpRepository.save(otpVerification);
        
        return true;
    }
    
    /**
     * Check if mobile number has a verified OTP (used before registration)
     */
    public boolean isMobileVerified(String mobileNumber) {
        // Check if there's a verified OTP within the last 30 minutes
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        
        return otpRepository.findByMobileNumber(mobileNumber).stream()
                .anyMatch(otp -> otp.getVerified() 
                        && otp.getCreatedAt().isAfter(thirtyMinutesAgo));
    }
    
    /**
     * Clean up expired OTPs (scheduled task)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }
    
    /**
     * Generate random OTP
     */
    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    /**
     * Send OTP via SMS
     */
    private void sendOtpViaSms(String mobileNumber, String otp) {
        if (smsService != null) {
            String message = String.format(
                    "Your OTP for Vehicle Booking App is: %s. Valid for %d minutes.",
                    otp, otpExpiryMinutes);
            smsService.sendSms(mobileNumber, message);
        } else {
            // In development/testing, just log it
            System.out.println("=================================");
            System.out.println("OTP for " + mobileNumber + ": " + otp);
            System.out.println("Expires at: " + LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            System.out.println("=================================");
        }
    }
    
    /**
     * Validate mobile number format
     */
    private boolean isValidMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return false;
        }
        // 10-15 digits only
        return mobileNumber.matches("^[0-9]{10,15}$");
    }
}