package com.alvexo.bookingapp.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.UnauthorizedException;

import jakarta.annotation.PostConstruct;

/**
 * SMS OTP delivery via Twilio Verify. Twilio generates, stores, and expires
 * the code itself — there is no local OTP record for these flows, unlike the
 * email-based {@link OtpService}.
 */
@Service
public class TwilioVerifyService {

    private static final String INDIA_COUNTRY_CODE = "+91";

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.verify.service.sid}")
    private String verifyServiceSid;

    private boolean configured;

    @PostConstruct
    private void init() {
        configured = !accountSid.isBlank() && !authToken.isBlank() && !verifyServiceSid.isBlank();
        if (configured) {
            Twilio.init(accountSid, authToken);
        }
    }

    /** Sends an SMS OTP to the given 10-digit mobile number. */
    public void startVerification(String mobileNumber) {
        requireConfigured();
        try {
            Verification.creator(verifyServiceSid, toE164(mobileNumber), "sms").create();
        } catch (Exception e) {
            throw new BusinessRuleException("OTP_DELIVERY_FAILED", "Failed to send OTP via SMS: " + e.getMessage());
        }
    }

    /**
     * Validates the OTP the user submitted for the given mobile number.
     *
     * @throws UnauthorizedException if the code is wrong, expired, or already used
     */
    public void checkVerification(String mobileNumber, String code) {
        requireConfigured();
        VerificationCheck check;
        try {
            check = VerificationCheck.creator(verifyServiceSid)
                    .setTo(toE164(mobileNumber))
                    .setCode(code)
                    .create();
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
        if (!"approved".equals(check.getStatus())) {
            throw new UnauthorizedException("Invalid or expired OTP");
        }
    }

    private void requireConfigured() {
        if (!configured) {
            throw new BusinessRuleException("OTP_DELIVERY_FAILED",
                    "SMS OTP delivery is not configured (missing Twilio credentials)");
        }
    }

    private static String toE164(String mobileNumber) {
        return INDIA_COUNTRY_CODE + mobileNumber;
    }
}
