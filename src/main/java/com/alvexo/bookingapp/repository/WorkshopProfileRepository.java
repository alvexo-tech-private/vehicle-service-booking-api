package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopProfileRepository extends JpaRepository<WorkshopProfile, Long> {
    Optional<WorkshopProfile> findByMechanic(User mechanic);
    Optional<WorkshopProfile> findByMechanicId(Long mechanicId);
}
