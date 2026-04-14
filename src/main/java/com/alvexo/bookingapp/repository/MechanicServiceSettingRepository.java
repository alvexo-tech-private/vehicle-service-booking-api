package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicServiceSetting;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicServiceSettingRepository extends JpaRepository<MechanicServiceSetting, Long> {

    /** All active services for a mechanic, ordered for UI display */
    List<MechanicServiceSetting> findByMechanicAndIsActiveTrueOrderByDisplayOrderAsc(User mechanic);

    /** All services (including inactive) — for mechanic's own management view */
    List<MechanicServiceSetting> findByMechanicOrderByDisplayOrderAsc(User mechanic);

    Optional<MechanicServiceSetting> findByIdAndMechanic(Long id, User mechanic);

    /** Total booked duration in minutes for a mechanic on a given date (capacity check) */
    @Query("""
            SELECT COALESCE(SUM(s.durationMinutes), 0)
            FROM MechanicServiceSetting s
            JOIN Booking b ON b.serviceSetting.id = s.id
            WHERE b.mechanic.id = :mechanicId
              AND CAST(b.scheduledDateTime AS LocalDate) = :bookingDate
              AND b.status NOT IN ('CANCELLED', 'REJECTED')
            """)
    Integer sumBookedMinutesForDate(@Param("mechanicId") Long mechanicId,
                                    @Param("bookingDate") java.time.LocalDate bookingDate);

    /** Count of bookings for a specific service on a date (per-service cap check) */
    @Query("""
            SELECT COUNT(b)
            FROM Booking b
            WHERE b.serviceSetting.id = :serviceId
              AND CAST(b.scheduledDateTime AS LocalDate) = :bookingDate
              AND b.status NOT IN ('CANCELLED', 'REJECTED')
            """)
    int countBookingsForServiceOnDate(@Param("serviceId") Long serviceId,
                                      @Param("bookingDate") java.time.LocalDate bookingDate);
}
