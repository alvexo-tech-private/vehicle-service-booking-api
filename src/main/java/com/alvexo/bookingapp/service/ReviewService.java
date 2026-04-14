package com.alvexo.bookingapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.ReviewRepository;
import com.alvexo.bookingapp.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReviewService {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public Review createReview(Long bookingId, Integer rating, String comment, User vehicleUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        if (!booking.getVehicleUser().getId().equals(vehicleUser.getId())) {
            throw new BadRequestException("You can only review your own bookings");
        }
        
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("You can only review completed bookings");
        }
        
        if (reviewRepository.findByBooking(booking).isPresent()) {
            throw new BadRequestException("You have already reviewed this booking");
        }
        
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        
        Review review = Review.builder()
                .booking(booking)
                .mechanic(booking.getMechanic())
                .vehicleUser(vehicleUser)
                .rating(rating)
                .comment(comment)
                .build();
        
        review = reviewRepository.save(review);
        
        // Update mechanic rating
        updateMechanicRating(booking.getMechanic());
        
        // Send notification to mechanic
        notificationService.createNotification(
                booking.getMechanic(),
                "New Review Received",
                "You received a " + rating + " star review",
                NotificationType.REVIEW_RECEIVED,
                "Review",
                review.getId()
        );
        
        return review;
    }
    
    @Transactional
    public Review respondToReview(Long reviewId, String response, User mechanic) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only respond to your own reviews");
        }
        
        review.setResponse(response);
        review.setResponseDate(LocalDateTime.now());
        
        return reviewRepository.save(review);
    }
    
    public Page<Review> getMechanicReviews(Long mechanicId, Pageable pageable) {
        User mechanic = userRepository.findById(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));
        
        return reviewRepository.findByMechanic(mechanic, pageable);
    }
    
    private void updateMechanicRating(User mechanic) {
        Double avgRating = reviewRepository.getAverageRatingForMechanic(mechanic);
        if (avgRating != null) {
            mechanic.setRating(BigDecimal.valueOf(avgRating));
            mechanic.setTotalReviews(reviewRepository.findByMechanic(mechanic).size());
            userRepository.save(mechanic);
        }
    }
}
