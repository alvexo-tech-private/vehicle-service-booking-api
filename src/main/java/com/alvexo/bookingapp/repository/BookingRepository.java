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
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    List<Booking> findByVehicleUser(User vehicleUser);
    List<Booking> findByMechanic(User mechanic);
    Page<Booking> findByVehicleUser(User vehicleUser, Pageable pageable);
    Page<Booking> findByMechanic(User mechanic, Pageable pageable);
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Returns all active bookings for a mechanic in a date range.
     * Used to compute already-taken slots when showing availability.
     * REQUESTED/PENDING_PAYMENT/EXPIRED never held real capacity, so they're
     * excluded the same way CANCELLED/REJECTED are (§5 two-stage flow).
     */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :startDate AND :endDate " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED')")
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
           "AND b.status NOT IN ('CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED')")
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
           "AND b.status NOT IN ('CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED')")
    List<Booking> findAndLockConflicting(@Param("mechanic") User mechanic,
                                          @Param("scheduledDateTime") LocalDateTime scheduledDateTime);

 // 1. Counts active bookings for vehicle-count mode capacity check
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.mechanic = :mechanic
              AND CAST(b.scheduledDateTime AS LocalDate) = :date
              AND b.status NOT IN ('CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED')
            """)
    long countActiveBookingsForMechanicOnDate(@Param("mechanic") User mechanic,
                                              @Param("date") LocalDate date);

 // 2. Used by job card number generator to produce a daily sequence
    long countByMechanicAndJobCardNumberStartingWith(User mechanic, String prefix);

    /** All completed bookings for a mechanic, most recent first — used to derive service-due reminders per vehicle. */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic AND b.status = 'COMPLETED' " +
           "ORDER BY b.completedAt DESC")
    List<Booking> findCompletedBookingsByMechanicOrderByCompletedAtDesc(@Param("mechanic") User mechanic);

    /** All-time booking count for a mechanic — dashboard "totalBookingsTillDate" stat. */
    long countByMechanic(User mechanic);

    /** All-time cancelled bookings for a mechanic — used to compute the "cancelled after cutoff" dashboard stat. */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic AND b.status = 'CANCELLED' AND b.cancelledAt IS NOT NULL")
    List<Booking> findCancelledBookingsByMechanic(@Param("mechanic") User mechanic);

    /** Bookings stuck in PENDING_PAYMENT past their confirmation window — lazily expired on access (§5). */
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_PAYMENT' " +
           "AND b.confirmationExpiresAt IS NOT NULL AND b.confirmationExpiresAt < :now")
    List<Booking> findExpiredPendingPaymentBookings(@Param("now") LocalDateTime now);

    // ── Service Desk ─────────────────────────────────────────────────────────

    Optional<Booking> findByBookingNumberAndMechanic(String bookingNumber, User mechanic);

    Optional<Booking> findByJobCardNumber(String jobCardNumber);

    /** All non-null job card numbers already issued to this mechanic — used to compute the next number (§1.6). */
    @Query("SELECT b.jobCardNumber FROM Booking b WHERE b.mechanic = :mechanic AND b.jobCardNumber IS NOT NULL")
    List<String> findJobCardNumbersByMechanic(@Param("mechanic") User mechanic);

    /**
     * Today tab (§1.3): every booking still relevant to the given service day —
     * either scheduled for that date, or carried over unfinished from an
     * earlier day. PENDING/REQUESTED/PENDING_PAYMENT (awaiting approval/confirmation)
     * and EXPIRED bookings are never shown here.
     */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.status NOT IN ('PENDING', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "AND (b.isCarryOver = true OR CAST(b.scheduledDateTime AS LocalDate) = :date) " +
           "ORDER BY b.scheduledDateTime ASC")
    List<Booking> findTodayWorkspaceBookings(@Param("mechanic") User mechanic, @Param("date") LocalDate date);

    /**
     * Service Week tab (§2): future-dated bookings only, excluding PENDING/REQUESTED/
     * PENDING_PAYMENT (not yet approved/confirmed) and REJECTED/EXPIRED (never confirmed).
     */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :from AND :to " +
           "AND b.status NOT IN ('PENDING', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "ORDER BY b.scheduledDateTime ASC")
    List<Booking> findServiceWeekBookings(@Param("mechanic") User mechanic,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    /** Active (non-cancelled/rejected/pending/...) bookings for a mechanic on a date — used for Service Week capacity. */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND CAST(b.scheduledDateTime AS LocalDate) = :date " +
           "AND b.status NOT IN ('PENDING', 'REJECTED', 'CANCELLED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED')")
    long countActiveByMechanicAndDate(@Param("mechanic") User mechanic, @Param("date") LocalDate date);

    /** As above, restricted to rider-originated channels — used to compute the "riderBooked" split (§2.2). */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND CAST(b.scheduledDateTime AS LocalDate) = :date " +
           "AND b.status NOT IN ('PENDING', 'REJECTED', 'CANCELLED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "AND b.channel IN ('ONLINE', 'RIDER_APP')")
    long countActiveRiderBookedByMechanicAndDate(@Param("mechanic") User mechanic, @Param("date") LocalDate date);

    /** Pickup tab (§3): today's bookings requiring pickup. No history — status changes don't remove a row. */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND CAST(b.scheduledDateTime AS LocalDate) = :date " +
           "AND b.pickupRequired = true AND b.status NOT IN ('PENDING', 'CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "ORDER BY b.scheduledDateTime ASC")
    List<Booking> findPickups(@Param("mechanic") User mechanic, @Param("date") LocalDate date);

    /** Drop tab (§4): today's bookings requiring drop. No history — status changes don't remove a row. */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND CAST(b.scheduledDateTime AS LocalDate) = :date " +
           "AND b.dropRequired = true AND b.status NOT IN ('PENDING', 'CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "ORDER BY b.scheduledDateTime ASC")
    List<Booking> findDrops(@Param("mechanic") User mechanic, @Param("date") LocalDate date);

    /**
     * Daily Advance Summary (BACKEND_REQUIREMENTS_FULL_APP_WORKSHOP_RIDER.md §9): every valid
     * booking for the service day, including zero-advance ones. Cancelled/rejected/pending
     * (and not-yet-confirmed two-stage) bookings are excluded entirely.
     */
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND CAST(b.scheduledDateTime AS LocalDate) = :date " +
           "AND b.status NOT IN ('PENDING', 'CANCELLED', 'REJECTED', 'REQUESTED', 'PENDING_PAYMENT', 'EXPIRED') " +
           "ORDER BY b.scheduledDateTime ASC")
    List<Booking> findDailyAdvanceSummaryBookings(@Param("mechanic") User mechanic, @Param("date") LocalDate date);
}
