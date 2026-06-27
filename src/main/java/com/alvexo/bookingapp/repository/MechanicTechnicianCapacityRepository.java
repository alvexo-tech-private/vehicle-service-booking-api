package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.MechanicTechnicianCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MechanicTechnicianCapacityRepository
        extends JpaRepository<MechanicTechnicianCapacity, Long> {

    List<MechanicTechnicianCapacity> findByMechanicSettings(MechanicSettings settings);

    List<MechanicTechnicianCapacity> findByMechanicSettingsAndIsActiveTrue(
            MechanicSettings settings);

    @Query("SELECT COALESCE(SUM(t.reservedHours), 0) FROM MechanicTechnicianCapacity t " +
           "WHERE t.mechanicSettings = :settings AND t.isActive = true")
    BigDecimal sumActiveReservedHours(@Param("settings") MechanicSettings settings);
}
