package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.DailyQuotaOverride;
import com.alvexo.bookingapp.model.MechanicSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuotaOverrideRepository extends JpaRepository<DailyQuotaOverride, Long> {

    Optional<DailyQuotaOverride> findByMechanicSettingsAndOverrideDateAndIsActiveTrue(
            MechanicSettings settings, LocalDate date);

    List<DailyQuotaOverride> findByMechanicSettingsOrderByOverrideDateDesc(
            MechanicSettings settings);
}
