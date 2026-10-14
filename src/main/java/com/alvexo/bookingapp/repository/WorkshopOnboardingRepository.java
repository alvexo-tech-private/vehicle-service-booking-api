package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopOnboardingRepository extends JpaRepository<WorkshopOnboarding, Long> {
    Optional<WorkshopOnboarding> findByMechanic(User mechanic);
    Optional<WorkshopOnboarding> findByMechanicId(Long mechanicId);
}
