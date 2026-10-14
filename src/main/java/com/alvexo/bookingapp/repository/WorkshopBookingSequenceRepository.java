package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopBookingSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopBookingSequenceRepository extends JpaRepository<WorkshopBookingSequence, Long> {

    /**
     * Locked read used while minting the next bookingNumber — prevents two concurrent bookings
     * for the same workshop from being handed the same sequence number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM WorkshopBookingSequence s WHERE s.mechanic = :mechanic")
    Optional<WorkshopBookingSequence> findByMechanicForUpdate(@Param("mechanic") User mechanic);

    /** Permanently assigns the next 4-digit workshop code (sequence starts at 1888). */
    @Query(value = "SELECT nextval('workshop_code_seq')", nativeQuery = true)
    Long nextWorkshopCode();
}
