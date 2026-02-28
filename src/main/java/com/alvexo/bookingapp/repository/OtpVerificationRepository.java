package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.OtpVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    /**
     * Find the latest OTP for a mobile number
     */
    Optional<OtpVerification> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
    
    /**
     * Find the latest valid (not verified) OTP for a mobile number
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.mobileNumber = :mobileNumber " +
           "AND o.verified = false AND o.expiryTime > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findLatestValidOtp(String mobileNumber, LocalDateTime now);
    
    /**
     * Find all OTPs for a mobile number
     */
    List<OtpVerification> findByMobileNumber(String mobileNumber);
    
    /**
     * Delete expired OTPs (for cleanup)
     */
    void deleteByExpiryTimeBefore(LocalDateTime dateTime);
    
    /**
     * Count unverified OTPs for a mobile number (to prevent spam)
     */
    long countByMobileNumberAndVerifiedFalseAndCreatedAtAfter(
            String mobileNumber, LocalDateTime since);
}