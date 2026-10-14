package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.Settlement;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByMechanicAndSettlementDate(User mechanic, LocalDate settlementDate);

    Optional<Settlement> findByIdAndMechanic(Long id, User mechanic);

    List<Settlement> findByMechanicOrderBySettlementDateDesc(User mechanic);

    List<Settlement> findByMechanicAndSettlementDateBetweenOrderBySettlementDateDesc(
            User mechanic, LocalDate from, LocalDate to);
}
