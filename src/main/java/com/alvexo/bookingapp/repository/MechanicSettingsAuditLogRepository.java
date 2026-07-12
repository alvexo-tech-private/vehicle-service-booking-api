package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicSettingsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MechanicSettingsAuditLogRepository extends JpaRepository<MechanicSettingsAuditLog, Long> {

    List<MechanicSettingsAuditLog> findByMechanicIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long mechanicId, LocalDateTime from, LocalDateTime to);

    List<MechanicSettingsAuditLog> findTop50ByMechanicIdOrderByChangedAtDesc(Long mechanicId);
}
