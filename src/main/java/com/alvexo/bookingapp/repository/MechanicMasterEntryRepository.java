package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.MechanicMasterEntry;
import com.alvexo.bookingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicMasterEntryRepository extends JpaRepository<MechanicMasterEntry, Long> {

    List<MechanicMasterEntry> findByMechanicOrderByDisplayOrderAsc(User mechanic);

    List<MechanicMasterEntry> findByMechanicIdOrderByDisplayOrderAsc(Long mechanicId);

    Optional<MechanicMasterEntry> findByIdAndMechanic(Long id, User mechanic);
}
