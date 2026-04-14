package com.alvexo.bookingapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.Payment;
import com.alvexo.bookingapp.model.PaymentStatus;
import com.alvexo.bookingapp.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser(User user);
    Page<Payment> findByUser(User user, Pageable pageable);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findByStatus(PaymentStatus status);
}
