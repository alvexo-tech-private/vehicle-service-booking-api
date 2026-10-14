package com.alvexo.bookingapp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.ServiceEligibilityEntryRequest;
import com.alvexo.bookingapp.dto.response.ServiceEligibilityEntryResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.FuelType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.WorkshopServiceEligibility;
import com.alvexo.bookingapp.repository.WorkshopServiceEligibilityRepository;

import lombok.RequiredArgsConstructor;

/**
 * Workshop vehicle-make coverage (WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.1)
 * — which makes/categories a workshop services, consumed by rider search to compute
 * `isBrandSupported` / `supportedBrands`.
 */
@Service
@RequiredArgsConstructor
public class WorkshopServiceEligibilityService {

    private final WorkshopServiceEligibilityRepository eligibilityRepository;

    @Transactional(readOnly = true)
    public List<ServiceEligibilityEntryResponse> getEligibility(Long mechanicId) {
        return eligibilityRepository.findByMechanicId(mechanicId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ServiceEligibilityEntryResponse> saveEligibility(
            User mechanic, List<ServiceEligibilityEntryRequest> entries) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can configure service eligibility");
        }

        List<WorkshopServiceEligibility> saved = new ArrayList<>();
        for (ServiceEligibilityEntryRequest entry : entries) {
            WorkshopServiceEligibility eligibility = eligibilityRepository
                    .findByMechanicAndFuelTypeAndServiceCategory(mechanic, entry.getFuelType(), entry.getServiceCategory())
                    .orElseGet(() -> WorkshopServiceEligibility.builder()
                            .mechanic(mechanic)
                            .fuelType(entry.getFuelType())
                            .serviceCategory(entry.getServiceCategory())
                            .build());

            eligibility.setVehicleMakes(toCsv(entry.getVehicleMakes()));
            eligibility.setOtherMakesAllowed(Boolean.TRUE.equals(entry.getOtherMakesAllowed()));
            saved.add(eligibilityRepository.save(eligibility));
        }
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * True when the workshop services {@code vehicleMake} for {@code fuelType} in any
     * configured category. Permissive (returns true) when the workshop hasn't configured
     * eligibility at all yet, so existing bookable workshops aren't silently hidden.
     */
    @Transactional(readOnly = true)
    public boolean isBrandSupported(Long mechanicId, String vehicleMake, FuelType fuelType) {
        List<WorkshopServiceEligibility> entries = fuelType != null
                ? eligibilityRepository.findByMechanicIdAndFuelType(mechanicId, fuelType)
                : eligibilityRepository.findByMechanicId(mechanicId);

        if (entries.isEmpty()) {
            return true;
        }
        return entries.stream().anyMatch(e ->
                Boolean.TRUE.equals(e.getOtherMakesAllowed()) || toList(e.getVehicleMakes()).stream()
                        .anyMatch(make -> make.equalsIgnoreCase(vehicleMake)));
    }

    /** Flattened, de-duplicated list of every make the workshop has configured, across categories. */
    @Transactional(readOnly = true)
    public List<String> getSupportedBrands(Long mechanicId) {
        Set<String> brands = new LinkedHashSet<>();
        eligibilityRepository.findByMechanicId(mechanicId)
                .forEach(e -> brands.addAll(toList(e.getVehicleMakes())));
        return new ArrayList<>(brands);
    }

    private ServiceEligibilityEntryResponse toResponse(WorkshopServiceEligibility e) {
        return new ServiceEligibilityEntryResponse(
                e.getFuelType(), e.getServiceCategory(), toList(e.getVehicleMakes()),
                Boolean.TRUE.equals(e.getOtherMakesAllowed()));
    }

    private static String toCsv(List<String> makes) {
        if (makes == null || makes.isEmpty()) return null;
        return makes.stream().filter(m -> m != null && !m.isBlank())
                .map(String::trim).collect(Collectors.joining(","));
    }

    private static List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
