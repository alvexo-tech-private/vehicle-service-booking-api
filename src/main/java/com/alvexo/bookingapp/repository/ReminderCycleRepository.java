package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.ReminderCycle;
import com.alvexo.bookingapp.model.ReminderCycleStatus;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderCycleRepository extends JpaRepository<ReminderCycle, Long> {

    /** The one non-CLOSED cycle for this (mechanic, vehicle, customer), if any. */
    @Query("SELECT c FROM ReminderCycle c WHERE c.mechanic = :mechanic AND c.vehicle = :vehicle " +
           "AND c.customer = :customer AND c.status <> 'CLOSED'")
    Optional<ReminderCycle> findOpenCycle(@Param("mechanic") User mechanic,
                                           @Param("vehicle") Vehicle vehicle,
                                           @Param("customer") User customer);

    List<ReminderCycle> findByMechanicAndStatusNotOrderByScheduledServiceDateDesc(User mechanic, ReminderCycleStatus status);
}
