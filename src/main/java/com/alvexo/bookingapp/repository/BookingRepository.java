package com.alvexo.bookingapp.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.User;

import jakarta.persistence.LockModeType;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    List<Booking> findByVehicleUser(User vehicleUser);
    List<Booking> findByMechanic(User mechanic);
    Page<Booking> findByVehicleUser(User vehicleUser, Pageable pageable);
    Page<Booking> findByMechanic(User mechanic, Pageable pageable);
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Returns all active bookings for a mechanic in a date range.
     * Used to compute already-taken slots when showing availability.
     */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :startDate AND :endDate " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    List<Booking> findMechanicBookingsBetween(@Param("mechanic") User mechanic,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Returns just the booked start-times for a mechanic on a given day.
     * Used during slot computation to exclude already-taken times.
     */
    @Query("SELECT b.scheduledDateTime FROM Booking b " +
           "WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :dayStart AND :dayEnd " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    Set<LocalDateTime> findBookedDateTimes(@Param("mechanic") User mechanic,
                                            @Param("dayStart") LocalDateTime dayStart,
                                            @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * Conflict check with PESSIMISTIC_WRITE lock.
     *
     * This is the core concurrency guard. Acquiring an exclusive row-level lock
     * on conflicting bookings before inserting a new one ensures that two
     * concurrent requests for the same slot cannot both pass the check and
     * both proceed to insert — one will wait for the other to commit, then
     * see the conflict and be rejected.
     *
     * Works together with the DB unique constraint on (mechanic_id, scheduled_date_time)
     * as a belt-and-suspenders approach.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime = :scheduledDateTime " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    List<Booking> findAndLockConflicting(@Param("mechanic") User mechanic,
                                          @Param("scheduledDateTime") LocalDateTime scheduledDateTime);
    
 // 1. Counts active bookings for vehicle-count mode capacity check
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.mechanic = :mechanic
              AND CAST(b.scheduledDateTime AS LocalDate) = :date
              AND b.status NOT IN ('CANCELLED', 'REJECTED')
            """)
    long countActiveBookingsForMechanicOnDate(@Param("mechanic") User mechanic,
                                              @Param("date") LocalDate date);
    
 // 2. Used by job card number generator to produce a daily sequence
    long countByMechanicAndJobCardNumberStartingWith(User mechanic, String prefix);
}
