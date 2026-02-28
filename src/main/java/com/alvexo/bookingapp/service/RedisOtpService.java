package com.alvexo.bookingapp.service;

import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisOtpService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int OTP_TTL_MINUTES = 5;

    private String otpKey(String mobile) {
        return "OTP:" + mobile;
    }

    private String attemptsKey(String mobile) {
        return "OTP_ATTEMPTS:" + mobile;
    }

    // Generate + Store OTP
    public String generateOtp(String mobile) {

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        redisTemplate.opsForValue().set(
                otpKey(mobile),
                otp,
                Duration.ofMinutes(OTP_TTL_MINUTES)
        );

        return otp;
    }

    // Validate OTP
    public boolean validateOtp(String mobile, String otp) {

        String savedOtp = redisTemplate.opsForValue().get(otpKey(mobile));

        if (savedOtp == null) {
            throw new RuntimeException("OTP expired or not found");
        }

        if (!savedOtp.equals(otp)) {

            incrementAttempts(mobile);
            throw new RuntimeException("Invalid OTP");
        }

        redisTemplate.delete(otpKey(mobile));
        redisTemplate.delete(attemptsKey(mobile));

        return true;
    }

    // Rate limiting (Max 5 attempts)
    private void incrementAttempts(String mobile) {

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey(mobile));

        if (attempts == 1) {
            redisTemplate.expire(attemptsKey(mobile), Duration.ofMinutes(10));
        }

        if (attempts > 5) {
            throw new RuntimeException("Too many attempts. Try later.");
        }
    }
}
