package com.alvexo.bookingapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.MechanicSettingsRestorePoint;
import com.alvexo.bookingapp.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicSettingsRestorePointRepository extends JpaRepository<MechanicSettingsRestorePoint, Long> {

    List<MechanicSettingsRestorePoint> findByMechanicOrderBySavedAtDesc(User mechanic);

    Optional<MechanicSettingsRestorePoint> findByIdAndMechanic(Long id, User mechanic);
}
