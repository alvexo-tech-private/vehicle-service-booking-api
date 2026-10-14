package com.alvexo.bookingapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.MechanicDailyServiceCapacity;

@Repository
public interface MechanicDailyServiceCapacityRepository extends JpaRepository<MechanicDailyServiceCapacity, Long> {

    List<MechanicDailyServiceCapacity> findByDailyOverride(MechanicDailyOverride dailyOverride);

    void deleteByDailyOverride(MechanicDailyOverride dailyOverride);
}
