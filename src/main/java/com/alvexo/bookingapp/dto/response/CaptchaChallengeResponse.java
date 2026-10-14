package com.alvexo.bookingapp.dto.response;

import java.time.LocalDateTime;

public record CaptchaChallengeResponse(
        String captchaId,
        String captchaCode,
        LocalDateTime expiresAt
) {}
