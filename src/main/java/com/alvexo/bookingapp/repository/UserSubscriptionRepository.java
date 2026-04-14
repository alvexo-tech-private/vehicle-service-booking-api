package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.SubscriptionStatus;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserSubscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUser(User user);
    Optional<UserSubscription> findByUserAndStatus(User user, SubscriptionStatus status);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.user = :user " +
           "AND us.status = 'ACTIVE' AND us.endDate > :now")
    Optional<UserSubscription> findActiveSubscription(User user, LocalDateTime now);
    
    List<UserSubscription> findByEndDateBeforeAndStatus(LocalDateTime date, SubscriptionStatus status);
}
