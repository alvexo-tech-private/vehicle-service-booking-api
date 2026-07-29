package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopLegalConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkshopLegalConsentRepository extends JpaRepository<WorkshopLegalConsent, Long> {
    List<WorkshopLegalConsent> findByMechanicOrderByAcceptedAtDesc(User mechanic);
}
