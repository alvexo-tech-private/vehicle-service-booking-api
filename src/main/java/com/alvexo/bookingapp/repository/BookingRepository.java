package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumber(String bookingNumber);
    List<Booking> findByVehicleUser(User vehicleUser);
    List<Booking> findByMechanic(User mechanic);
    Page<Booking> findByVehicleUser(User vehicleUser, Pageable pageable);
    Page<Booking> findByMechanic(User mechanic, Pageable pageable);
    List<Booking> findByStatus(BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :startDate AND :endDate")
    List<Booking> findMechanicBookingsBetween(@Param("mechanic") User mechanic,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.mechanic = :mechanic " +
           "AND b.scheduledDateTime BETWEEN :startTime AND :endTime " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    long countConflictingBookings(@Param("mechanic") User mechanic,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
