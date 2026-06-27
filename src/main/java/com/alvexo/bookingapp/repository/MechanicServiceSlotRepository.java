package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicServiceSlot;
import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MechanicServiceSlotRepository extends JpaRepository<MechanicServiceSlot, Long> {

    List<MechanicServiceSlot> findByMechanicSettings(MechanicSettings settings);

    List<MechanicServiceSlot> findByMechanicSettingsAndIsEnabledTrue(MechanicSettings settings);

    List<MechanicServiceSlot> findByMechanicSettingsAndRestrictedCategoryAndIsEnabledTrue(
            MechanicSettings settings, ServiceCategory category);
}
