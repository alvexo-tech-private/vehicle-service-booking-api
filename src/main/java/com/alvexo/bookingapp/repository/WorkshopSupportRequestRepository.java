package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopSupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkshopSupportRequestRepository extends JpaRepository<WorkshopSupportRequest, Long> {
    List<WorkshopSupportRequest> findByMechanicOrderByCreatedAtDesc(User mechanic);
}
