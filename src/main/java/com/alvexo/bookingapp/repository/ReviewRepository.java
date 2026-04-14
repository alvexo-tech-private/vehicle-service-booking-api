package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.Review;
import com.alvexo.bookingapp.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBooking(Booking booking);
    List<Review> findByMechanic(User mechanic);
    Page<Review> findByMechanic(User mechanic, Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mechanic = :mechanic")
    Double getAverageRatingForMechanic(User mechanic);
}
