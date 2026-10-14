package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopNotificationPreferencesRepository extends JpaRepository<WorkshopNotificationPreferences, Long> {
    Optional<WorkshopNotificationPreferences> findByMechanic(User mechanic);
}
