package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicHoliday;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicHolidayRepository extends JpaRepository<MechanicHoliday, Long> {

    List<MechanicHoliday> findByMechanicIdAndDateBetweenOrderByDateAsc(Long mechanicId, LocalDate from, LocalDate to);

    Optional<MechanicHoliday> findByMechanicAndDate(User mechanic, LocalDate date);

    Optional<MechanicHoliday> findByIdAndMechanic(Long id, User mechanic);

    boolean existsByMechanicAndDate(User mechanic, LocalDate date);
}
