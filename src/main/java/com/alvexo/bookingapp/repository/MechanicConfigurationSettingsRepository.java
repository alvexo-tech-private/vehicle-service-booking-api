package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MechanicConfigurationSettingsRepository extends JpaRepository<MechanicConfigurationSettings, Long> {

    Optional<MechanicConfigurationSettings> findByMechanic(User mechanic);

    Optional<MechanicConfigurationSettings> findByMechanicId(Long mechanicId);
}
