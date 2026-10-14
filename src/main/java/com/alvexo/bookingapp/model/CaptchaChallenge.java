package com.alvexo.bookingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Short-lived numeric challenge required before an OTP is issued
 * (BACKEND_SPECIFICATIONS_AND_REQUIREMENTS.md §2) — prevents SMS bombing by
 * forcing a human-solvable step before /api/auth/request-otp fires an SMS.
 */
@Entity
@Table(name = "captcha_challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptchaChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "captcha_id", nullable = false, unique = true, length = 36)
    private String captchaId;

    @Column(name = "code", nullable = false, length = 5)
    private String code;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
