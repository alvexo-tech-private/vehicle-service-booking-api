package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WorkshopImage;
import com.alvexo.bookingapp.model.WorkshopImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopImageRepository extends JpaRepository<WorkshopImage, Long> {
    List<WorkshopImage> findByMechanicOrderByUploadedAtAsc(User mechanic);
    Optional<WorkshopImage> findByIdAndMechanic(Long id, User mechanic);
    boolean existsByMechanicAndType(User mechanic, WorkshopImageType type);
    long countByMechanic(User mechanic);
}
