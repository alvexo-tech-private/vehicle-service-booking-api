package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.DayOfWeek;
import com.alvexo.bookingapp.model.MechanicAvailability;
import com.alvexo.bookingapp.model.User;

import java.util.List;

@Repository
public interface MechanicAvailabilityRepository extends JpaRepository<MechanicAvailability, Long> {
    List<MechanicAvailability> findByMechanic(User mechanic);
    List<MechanicAvailability> findByMechanicAndDayOfWeek(User mechanic, DayOfWeek dayOfWeek);
    List<MechanicAvailability> findByMechanicAndIsAvailableTrue(User mechanic);
}
