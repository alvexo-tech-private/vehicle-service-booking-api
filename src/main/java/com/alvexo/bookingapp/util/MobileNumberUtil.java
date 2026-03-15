package com.alvexo.bookingapp.util;

/**
 * Utility for normalising mobile numbers before they are stored or looked up.
 *
 * <p>Accepted input formats (all produce the same stored value):
 * <pre>
 *   9876543210        → 9876543210   (plain 10-digit)
 *   +919876543210     → 9876543210   (E.164 with leading +)
 *   919876543210      → 9876543210   (country code without +)
 *   0919876543210     → 9876543210   (IDD prefix 0)
 *   09876543210       → 9876543210   (local trunk prefix 0)
 * </pre>
 *
 * <p>The normalisation rule is simple and intentional:
 * <ol>
 *   <li>Strip any leading {@code +} or {@code 0} characters.</li>
 *   <li>If the resulting string is longer than 10 digits, assume the extra
 *       leading digits are a country code and drop them, keeping the last 10.</li>
 * </ol>
 *
 * <p>Only digits are accepted after the optional leading {@code +}/{@code 0}.
 * Spaces, dashes, and parentheses must be removed by the caller (or the DTO
 * {@code @Pattern} will already reject them).
 */
public final class MobileNumberUtil {

    /** Digits stored per number — the "subscriber number" length used in India and most markets. */
    public static final int SUBSCRIBER_LENGTH = 10;

    private MobileNumberUtil() {
        // utility class
    }

    /**
     * Normalises {@code raw} to a {@value #SUBSCRIBER_LENGTH}-digit subscriber number.
     *
     * @param raw the mobile number as provided by the user; must not be {@code null}
     * @return the normalised 10-digit subscriber number
     * @throws IllegalArgumentException if {@code raw} is blank or contains non-digit
     *                                  characters (after stripping a leading {@code +})
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Mobile number must not be blank");
        }

        // Remove leading '+' (E.164 prefix)
        String digits = raw.startsWith("+") ? raw.substring(1) : raw;

        // Remove a single leading '0' (IDD or trunk prefix: 0091…, 091…, 09876…)
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // At this point we expect only digits
        if (!digits.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Mobile number contains invalid characters: " + raw);
        }

        // If longer than SUBSCRIBER_LENGTH, strip leading country-code digits
        if (digits.length() > SUBSCRIBER_LENGTH) {
            digits = digits.substring(digits.length() - SUBSCRIBER_LENGTH);
        }

        if (digits.length() != SUBSCRIBER_LENGTH) {
            throw new IllegalArgumentException(
                    "Mobile number must resolve to " + SUBSCRIBER_LENGTH + " digits, got: " + raw);
        }

        return digits;
    }
}
