package com.alvexo.bookingapp.exception;

import lombok.Getter;

/**
 * A 400-level failure that the client is expected to branch on programmatically
 * (e.g. "REFERRAL_EXPIRED", "ONBOARDING_INCOMPLETE") rather than just display.
 * See the Workshop Profile API error-code table.
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }
}
