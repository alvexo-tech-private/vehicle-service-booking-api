package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.JobCardSequence;
import com.alvexo.bookingapp.model.MechanicSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface JobCardSequenceRepository extends JpaRepository<JobCardSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM JobCardSequence s " +
           "WHERE s.mechanicSettings = :settings AND s.sequenceDate = :date")
    Optional<JobCardSequence> findAndLockByMechanicSettingsAndDate(
            @Param("settings") MechanicSettings settings,
            @Param("date") LocalDate date);
}
