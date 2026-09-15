package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopVerificationRepository extends JpaRepository<WorkshopVerification, Long> {
    Optional<WorkshopVerification> findByMechanic(User mechanic);
}
