package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicPromotionalOffer;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicPromotionalOfferRepository extends JpaRepository<MechanicPromotionalOffer, Long> {

    List<MechanicPromotionalOffer> findByMechanicIdOrderByStartDateDesc(Long mechanicId);

    Optional<MechanicPromotionalOffer> findByIdAndMechanic(Long id, User mechanic);

    @Query("""
            SELECT o FROM MechanicPromotionalOffer o
            WHERE o.mechanic.id = :mechanicId
              AND o.active = true
              AND o.startDate <= :today
              AND o.endDate >= :today
            ORDER BY o.startDate DESC
            """)
    List<MechanicPromotionalOffer> findActiveOffers(@Param("mechanicId") Long mechanicId,
                                                      @Param("today") LocalDate today);
}
