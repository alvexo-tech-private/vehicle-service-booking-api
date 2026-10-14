package com.alvexo.bookingapp.repository;

import com.alvexo.bookingapp.model.LegalDocument;
import com.alvexo.bookingapp.model.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    Optional<LegalDocument> findByDocumentType(LegalDocumentType documentType);
}
