package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.ServiceCapacityOverrideEntry;
import com.alvexo.bookingapp.dto.request.MechanicDailyOverrideRequest;
import com.alvexo.bookingapp.dto.response.MechanicDailyOverrideResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.MechanicDailyServiceCapacity;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicDailyOverrideRepository;
import com.alvexo.bookingapp.repository.MechanicDailyServiceCapacityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MechanicDailyOverrideService {

    private final MechanicDailyOverrideRepository overrideRepository;
    private final MechanicDailyServiceCapacityRepository serviceCapacityRepository;

    public MechanicDailyOverrideService(MechanicDailyOverrideRepository overrideRepository,
                                         MechanicDailyServiceCapacityRepository serviceCapacityRepository) {
        this.overrideRepository = overrideRepository;
        this.serviceCapacityRepository = serviceCapacityRepository;
    }

    @Transactional
    public MechanicDailyOverrideResponse saveOverride(User mechanic, MechanicDailyOverrideRequest request) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can set daily overrides");
        }
        boolean hasServiceCapacities = request.getServiceCapacities() != null && !request.getServiceCapacities().isEmpty();
        if (request.getMaxVehiclesPerDayOverride() == null
                && request.getFullDayCapacityHoursOverride() == null
                && request.getAdvanceEnabledOverride() == null
                && request.getAdvanceAmountOverride() == null
                && !hasServiceCapacities) {
            throw new BadRequestException("At least one override field must be provided");
        }

        MechanicDailyOverride override = overrideRepository.findByMechanicAndDate(mechanic, request.getDate())
                .orElse(MechanicDailyOverride.builder().mechanic(mechanic).date(request.getDate()).build());

        override.setMaxVehiclesPerDayOverride(request.getMaxVehiclesPerDayOverride());
        override.setFullDayCapacityHoursOverride(request.getFullDayCapacityHoursOverride());
        override.setAdvanceEnabledOverride(request.getAdvanceEnabledOverride());
        override.setAdvanceAmountOverride(request.getAdvanceAmountOverride());
        final MechanicDailyOverride savedOverride = overrideRepository.save(override);

        if (request.getServiceCapacities() != null) {
            serviceCapacityRepository.deleteByDailyOverride(savedOverride);
            List<MechanicDailyServiceCapacity> capacities = request.getServiceCapacities().stream()
                    .map(e -> MechanicDailyServiceCapacity.builder()
                            .dailyOverride(savedOverride)
                            .serviceName(e.getServiceType())
                            .capacity(e.getCapacity())
                            .build())
                    .collect(Collectors.toList());
            serviceCapacityRepository.saveAll(capacities);
        }

        return toResponse(savedOverride);
    }

    @Transactional
    public void deleteOverride(User mechanic, Long overrideId) {
        MechanicDailyOverride override = overrideRepository.findByIdAndMechanic(overrideId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Daily override not found"));
        overrideRepository.delete(override);
    }

    @Transactional(readOnly = true)
    public List<MechanicDailyOverrideResponse> getOverrides(Long mechanicId, LocalDate from, LocalDate to) {
        return overrideRepository.findByMechanicIdAndDateBetweenOrderByDateAsc(mechanicId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Used by BookingService/MechanicDashboardService to resolve effective capacity for a date. */
    @Transactional(readOnly = true)
    public Optional<MechanicDailyOverride> findOverride(User mechanic, LocalDate date) {
        return overrideRepository.findByMechanicAndDate(mechanic, date);
    }

    private MechanicDailyOverrideResponse toResponse(MechanicDailyOverride o) {
        List<ServiceCapacityOverrideEntry> serviceCapacities = serviceCapacityRepository.findByDailyOverride(o).stream()
                .map(c -> ServiceCapacityOverrideEntry.builder()
                        .serviceType(c.getServiceName())
                        .capacity(c.getCapacity())
                        .build())
                .collect(Collectors.toList());

        return MechanicDailyOverrideResponse.builder()
                .id(o.getId())
                .date(o.getDate())
                .maxVehiclesPerDayOverride(o.getMaxVehiclesPerDayOverride())
                .fullDayCapacityHoursOverride(o.getFullDayCapacityHoursOverride())
                .advanceEnabledOverride(o.getAdvanceEnabledOverride())
                .advanceAmountOverride(o.getAdvanceAmountOverride())
                .serviceCapacities(serviceCapacities)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
