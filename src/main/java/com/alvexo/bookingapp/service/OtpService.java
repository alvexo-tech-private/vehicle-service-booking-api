package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.OtpVerification;
import com.alvexo.bookingapp.repository.OtpVerificationRepository;
import com.alvexo.bookingapp.util.OtpUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {

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

    // ── Email-change OTP flow ─────────────────────────────────────────────────

    /**
     * Generates and persists an OTP record keyed by {@code newEmail}.
     * The caller's current mobile number is stored alongside for context.
     *
     * @param newEmail      the email address the user wants to switch to (used as lookup key)
     * @param currentMobile the user's existing mobile number (stored as non-key context)
     * @return the generated 6-digit OTP string (caller must deliver it to newEmail)
     */
    public String generateAndSaveOtpForEmail(String newEmail, String currentMobile) {
        String otp = OtpUtil.generateOtp();
        OtpVerification entity = OtpVerification.builder()
                .email(newEmail)
                .mobileNumber(currentMobile)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        otpRepository.save(entity);
        return otp;
    }

    /**
     * Validates the OTP for an email-change request.
     * Looks up the most recent record by {@code newEmail} and checks expiry and value.
     *
     * @param newEmail the target email that was used when the OTP was generated
     * @param otp      the code submitted by the user
     * @throws BadRequestException if no OTP record exists, the OTP is expired, or the value is wrong
     */
    public void validateOtpByEmail(String newEmail, String otp) {
        OtpVerification record = otpRepository
                .findTopByEmailOrderByCreatedAtDesc(newEmail)
                .orElseThrow(() -> new BadRequestException("OTP not found for the given email"));

        if (record.isExpired()) {
            throw new BadRequestException("OTP has expired. Please request a new one");
        }
        if (record.getVerified()) {
            throw new BadRequestException("OTP has already been used");
        }
        if (!record.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        record.setVerified(true);
        otpRepository.save(record);
    }

    // ── Mobile-change OTP flow ────────────────────────────────────────────────

    /**
     * Generates and persists an OTP record keyed by {@code newMobile}.
     * The caller's current email is stored alongside for context.
     *
     * @param newMobile    the mobile number the user wants to switch to (used as lookup key)
     * @param currentEmail the user's existing email address (stored as non-key context)
     * @return the generated 6-digit OTP string (caller must deliver it to the user)
     */
    public String generateAndSaveOtpForMobile(String newMobile, String currentEmail) {
        String otp = OtpUtil.generateOtp();
        OtpVerification entity = OtpVerification.builder()
                .mobileNumber(newMobile)
                .email(currentEmail)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        otpRepository.save(entity);
        return otp;
    }

    /**
     * Validates the OTP for a mobile-change request.
     * Reuses the existing mobile-keyed lookup; throws {@link BadRequestException}
     * with user-friendly messages (unlike the login flow which throws RuntimeException).
     *
     * @param newMobile the target mobile number that was used when the OTP was generated
     * @param otp       the code submitted by the user
     * @throws BadRequestException if no OTP record exists, the OTP is expired, or the value is wrong
     */
    public void validateOtpByMobile(String newMobile, String otp) {
        OtpVerification record = otpRepository
                .findTopByMobileNumberOrderByCreatedAtDesc(newMobile)
                .orElseThrow(() -> new BadRequestException("OTP not found for the given mobile number"));

        if (record.isExpired()) {
            throw new BadRequestException("OTP has expired. Please request a new one");
        }
        if (record.getVerified()) {
            throw new BadRequestException("OTP has already been used");
        }
        if (!record.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        record.setVerified(true);
        otpRepository.save(record);
    }
}
