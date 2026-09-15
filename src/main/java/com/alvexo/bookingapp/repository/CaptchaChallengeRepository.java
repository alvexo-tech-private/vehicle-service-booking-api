package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.CaptchaChallenge;

import java.util.Optional;

@Repository
public interface CaptchaChallengeRepository extends JpaRepository<CaptchaChallenge, Long> {

    Optional<CaptchaChallenge> findByCaptchaId(String captchaId);
}
