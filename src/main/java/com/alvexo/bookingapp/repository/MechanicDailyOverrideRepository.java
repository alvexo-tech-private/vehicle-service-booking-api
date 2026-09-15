package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicDailyOverrideRepository extends JpaRepository<MechanicDailyOverride, Long> {

    Optional<MechanicDailyOverride> findByMechanicAndDate(User mechanic, LocalDate date);

    List<MechanicDailyOverride> findByMechanicIdAndDateBetweenOrderByDateAsc(Long mechanicId, LocalDate from, LocalDate to);

    Optional<MechanicDailyOverride> findByIdAndMechanic(Long id, User mechanic);
}
