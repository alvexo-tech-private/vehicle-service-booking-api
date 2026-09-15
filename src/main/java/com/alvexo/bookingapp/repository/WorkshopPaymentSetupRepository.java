package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopPaymentSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopPaymentSetupRepository extends JpaRepository<WorkshopPaymentSetup, Long> {
    Optional<WorkshopPaymentSetup> findByMechanic(User mechanic);
}
