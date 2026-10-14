package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.alvexo.bookingapp.dto.response.CaptchaChallengeResponse;
import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.model.CaptchaChallenge;
import com.alvexo.bookingapp.repository.CaptchaChallengeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 5-digit numeric captcha challenge required before an OTP is issued
 * (BACKEND_SPECIFICATIONS_AND_REQUIREMENTS.md §2 — OTP misuse prevention).
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final int CODE_LENGTH_MIN = 10000;
    private static final int CODE_LENGTH_RANGE = 90000;

    private final CaptchaChallengeRepository captchaRepository;

    public CaptchaChallengeResponse generateChallenge() {
        String code = String.valueOf(CODE_LENGTH_MIN + new Random().nextInt(CODE_LENGTH_RANGE));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        CaptchaChallenge challenge = CaptchaChallenge.builder()
                .captchaId(UUID.randomUUID().toString())
                .code(code)
                .expiryTime(expiresAt)
                .used(false)
                .build();
        challenge = captchaRepository.save(challenge);

        return new CaptchaChallengeResponse(challenge.getCaptchaId(), challenge.getCode(), expiresAt);
    }

    /**
     * Validates and single-use-consumes a captcha. Throws BusinessRuleException
     * with errorCode "INVALID_CAPTCHA" on any failure (missing, expired, reused, or wrong code).
     */
    public void validateAndConsume(String captchaId, String response) {
        if (captchaId == null || captchaId.isBlank() || response == null || response.isBlank()) {
            throw new BusinessRuleException("INVALID_CAPTCHA", "Captcha challenge and response are required");
        }

        CaptchaChallenge challenge = captchaRepository.findByCaptchaId(captchaId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CAPTCHA",
                        "Incorrect 5-digit number entered. Please try again."));

        if (challenge.getUsed() || challenge.isExpired() || !challenge.getCode().equals(response.trim())) {
            throw new BusinessRuleException("INVALID_CAPTCHA",
                    "Incorrect 5-digit number entered. Please try again.");
        }

        challenge.setUsed(true);
        captchaRepository.save(challenge);
    }
}
