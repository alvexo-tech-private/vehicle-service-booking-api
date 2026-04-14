package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MechanicSettingsRepository extends JpaRepository<MechanicSettings, Long> {

    Optional<MechanicSettings> findByMechanic(User mechanic);

    Optional<MechanicSettings> findByMechanicId(Long mechanicId);

    boolean existsByMechanic(User mechanic);
}
