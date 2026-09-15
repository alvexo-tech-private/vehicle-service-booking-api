package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicServiceSlot;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicServiceSlotRepository extends JpaRepository<MechanicServiceSlot, Long> {

    List<MechanicServiceSlot> findByMechanicOrderBySlotNumberAsc(User mechanic);

    List<MechanicServiceSlot> findByMechanicIdOrderBySlotNumberAsc(Long mechanicId);

    Optional<MechanicServiceSlot> findByMechanicAndSlotNumber(User mechanic, Integer slotNumber);
}
