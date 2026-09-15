package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.MechanicPromotionalOfferRequest;
import com.alvexo.bookingapp.dto.response.MechanicPromotionalOfferResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicPromotionalOffer;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicPromotionalOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MechanicPromotionalOfferService {

    private final MechanicPromotionalOfferRepository offerRepository;

    public MechanicPromotionalOfferService(MechanicPromotionalOfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Transactional
    public MechanicPromotionalOfferResponse createOffer(User mechanic, MechanicPromotionalOfferRequest request) {
        validateRole(mechanic);
        validateDates(request);

        MechanicPromotionalOffer offer = MechanicPromotionalOffer.builder()
                .mechanic(mechanic)
                .title(request.getTitle())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toResponse(offerRepository.save(offer));
    }

    @Transactional
    public MechanicPromotionalOfferResponse updateOffer(User mechanic, Long offerId,
                                                          MechanicPromotionalOfferRequest request) {
        validateRole(mechanic);
        validateDates(request);

        MechanicPromotionalOffer offer = offerRepository.findByIdAndMechanic(offerId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Promotional offer not found"));

        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setDiscountType(request.getDiscountType());
        offer.setDiscountValue(request.getDiscountValue());
        offer.setStartDate(request.getStartDate());
        offer.setEndDate(request.getEndDate());
        if (request.getActive() != null) offer.setActive(request.getActive());

        return toResponse(offerRepository.save(offer));
    }

    @Transactional
    public void deleteOffer(User mechanic, Long offerId) {
        validateRole(mechanic);
        MechanicPromotionalOffer offer = offerRepository.findByIdAndMechanic(offerId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Promotional offer not found"));
        offerRepository.delete(offer);
    }

    @Transactional(readOnly = true)
    public List<MechanicPromotionalOfferResponse> getAllOffers(Long mechanicId) {
        return offerRepository.findByMechanicIdOrderByStartDateDesc(mechanicId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MechanicPromotionalOfferResponse> getActiveOffers(Long mechanicId) {
        return offerRepository.findActiveOffers(mechanicId, LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void validateDates(MechanicPromotionalOfferRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("endDate cannot be before startDate");
        }
        if (request.getDiscountType() == com.alvexo.bookingapp.model.DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("discountValue cannot exceed 100 for a PERCENTAGE offer");
        }
    }

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage promotional offers");
        }
    }

    private MechanicPromotionalOfferResponse toResponse(MechanicPromotionalOffer o) {
        return MechanicPromotionalOfferResponse.builder()
                .id(o.getId())
                .mechanicId(o.getMechanic().getId())
                .title(o.getTitle())
                .description(o.getDescription())
                .discountType(o.getDiscountType())
                .discountValue(o.getDiscountValue())
                .startDate(o.getStartDate())
                .endDate(o.getEndDate())
                .active(o.getActive())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
