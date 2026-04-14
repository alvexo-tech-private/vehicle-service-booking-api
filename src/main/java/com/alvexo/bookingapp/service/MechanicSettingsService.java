package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.MechanicServiceSettingRequest;
import com.alvexo.bookingapp.dto.request.MechanicSettingsRequest;
import com.alvexo.bookingapp.dto.response.MechanicServiceSettingResponse;
import com.alvexo.bookingapp.dto.response.MechanicSettingsResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MechanicSettingsService {

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;

    public MechanicSettingsService(MechanicSettingsRepository settingsRepository,
                                   MechanicServiceSettingRepository serviceSettingRepository) {
        this.settingsRepository = settingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
    }

    // ── Create or replace all settings (upsert) ───────────────────────────────

    @Transactional
    public MechanicSettingsResponse saveSettings(User mechanic, MechanicSettingsRequest request) {
        validateRole(mechanic);
        validateSettingsRequest(request);

        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElse(MechanicSettings.builder().mechanic(mechanic).build());

        applyRequest(settings, request);
        settings = settingsRepository.save(settings);

        // Replace service settings if provided
        if (request.getServiceSettings() != null) {
            replaceServiceSettings(mechanic, request.getServiceSettings());
        }

        return buildResponse(settings);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MechanicSettingsResponse getSettings(User mechanic) {
        validateRole(mechanic);
        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured yet"));
        return buildResponse(settings);
    }

    @Transactional(readOnly = true)
    public MechanicSettingsResponse getSettingsByMechanicId(Long mechanicId) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found for mechanic " + mechanicId));
        return buildResponse(settings);
    }

    // ── Service settings CRUD ─────────────────────────────────────────────────

    @Transactional
    public MechanicServiceSettingResponse addServiceSetting(User mechanic,
                                                             MechanicServiceSettingRequest request) {
        validateRole(mechanic);
        MechanicServiceSetting entity = buildServiceSetting(mechanic, request);
        return toServiceResponse(serviceSettingRepository.save(entity));
    }

    @Transactional
    public MechanicServiceSettingResponse updateServiceSetting(User mechanic, Long serviceId,
                                                                MechanicServiceSettingRequest request) {
        validateRole(mechanic);
        MechanicServiceSetting entity = serviceSettingRepository
                .findByIdAndMechanic(serviceId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));

        entity.setServiceName(request.getServiceName());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setMaxSlotsPerDay(request.getMaxSlotsPerDay());
        if (request.getIsExpressEligible() != null) entity.setIsExpressEligible(request.getIsExpressEligible());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());

        return toServiceResponse(serviceSettingRepository.save(entity));
    }

    @Transactional
    public void deleteServiceSetting(User mechanic, Long serviceId) {
        validateRole(mechanic);
        MechanicServiceSetting entity = serviceSettingRepository
                .findByIdAndMechanic(serviceId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));
        serviceSettingRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<MechanicServiceSettingResponse> getActiveServiceSettings(Long mechanicId) {
        // Used by booking flow to show available services to customers
        return serviceSettingRepository
                .findByMechanicAndIsActiveTrueOrderByDisplayOrderAsc(
                        resolveUser(mechanicId))
                .stream().map(this::toServiceResponse).collect(Collectors.toList());
    }

    // ── Capacity validation helpers (used by BookingService) ─────────────────

    /**
     * Vehicle-count mode: checks if mechanic still has vehicle slots for the date.
     */
    @Transactional(readOnly = true)
    public boolean hasVehicleSlotAvailable(Long mechanicId, java.time.LocalDate date, int activeBookingCount) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic settings not found"));
        return activeBookingCount < settings.getMaxVehiclesPerDay();
    }

    /**
     * Hour-slot mode: checks if adding durationMinutes would exceed fullDayCapacityHours.
     */
    @Transactional(readOnly = true)
    public boolean hasHourCapacityAvailable(Long mechanicId, java.time.LocalDate date, int newServiceDurationMinutes) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic settings not found"));

        int bookedMinutes = serviceSettingRepository.sumBookedMinutesForDate(mechanicId, date);
        int capacityMinutes = settings.getFullDayCapacityHours()
                .multiply(java.math.BigDecimal.valueOf(60))
                .intValue();

        return (bookedMinutes + newServiceDurationMinutes) <= capacityMinutes;
    }

    /**
     * Determines booking type based on scheduled time vs express reporting time.
     * Returns EXPRESS if reserveCapacity=true and time is before expressReportingTime.
     */
    @Transactional(readOnly = true)
    public BookingType resolveBookingType(Long mechanicId, java.time.LocalTime scheduledTime) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic settings not found"));

        if (Boolean.TRUE.equals(settings.getReserveCapacity())
                && settings.getExpressReportingTime() != null
                && scheduledTime.isBefore(settings.getExpressReportingTime())) {
            return BookingType.EXPRESS;
        }
        return BookingType.STANDARD;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage settings");
        }
    }

    private void validateSettingsRequest(MechanicSettingsRequest r) {
        if (Boolean.TRUE.equals(r.getReserveCapacity())) {
            if (r.getFullDayCapacityHours() == null) {
                throw new BadRequestException("fullDayCapacityHours is required when reserveCapacity is true");
            }
            if (r.getExpressReportingTime() == null) {
                throw new BadRequestException("expressReportingTime is required when reserveCapacity is true");
            }
//            if (r.getExpressReportingTime().isAfter(r.getServiceReportingTime())) {
//                throw new BadRequestException("expressReportingTime must be before serviceReportingTime");
//            }
        }
        if (Boolean.TRUE.equals(r.getAdvanceEnabled()) && r.getAdvanceAmount() == null) {
            throw new BadRequestException("advanceAmount is required when advanceEnabled is true");
        }
    }

    private void applyRequest(MechanicSettings s, MechanicSettingsRequest r) {
        s.setMaxVehiclesPerDay(r.getMaxVehiclesPerDay());
        s.setReserveCapacity(r.getReserveCapacity());
        s.setFullDayCapacityHours(r.getFullDayCapacityHours());
        s.setJobCardSerialPrefix(r.getJobCardSerialPrefix());
        s.setServiceReportingTime(r.getServiceReportingTime());
        s.setExpressReportingTime(r.getExpressReportingTime());
        s.setAdvanceEnabled(r.getAdvanceEnabled());
        s.setAdvanceAmount(r.getAdvanceAmount());
    }

    private void replaceServiceSettings(User mechanic, List<MechanicServiceSettingRequest> requests) {
        List<MechanicServiceSetting> existing =
                serviceSettingRepository.findByMechanicOrderByDisplayOrderAsc(mechanic);
        serviceSettingRepository.deleteAll(existing);

        List<MechanicServiceSetting> fresh = requests.stream()
                .map(r -> buildServiceSetting(mechanic, r))
                .collect(Collectors.toList());
        serviceSettingRepository.saveAll(fresh);
    }

    private MechanicServiceSetting buildServiceSetting(User mechanic, MechanicServiceSettingRequest r) {
        return MechanicServiceSetting.builder()
                .mechanic(mechanic)
                .serviceName(r.getServiceName())
                .durationMinutes(r.getDurationMinutes())
                .maxSlotsPerDay(r.getMaxSlotsPerDay())
                .isExpressEligible(r.getIsExpressEligible() != null ? r.getIsExpressEligible() : false)
                .isActive(r.getIsActive() != null ? r.getIsActive() : true)
                .displayOrder(r.getDisplayOrder() != null ? r.getDisplayOrder() : 0)
                .build();
    }

    private MechanicSettingsResponse buildResponse(MechanicSettings s) {
        List<MechanicServiceSettingResponse> services = serviceSettingRepository
                .findByMechanicOrderByDisplayOrderAsc(s.getMechanic())
                .stream().map(this::toServiceResponse).collect(Collectors.toList());

        return MechanicSettingsResponse.builder()
                .id(s.getId())
                .mechanicId(s.getMechanic().getId())
                .mechanicName(s.getMechanic().getFirstName() + " " + s.getMechanic().getLastName())
                .maxVehiclesPerDay(s.getMaxVehiclesPerDay())
                .reserveCapacity(s.getReserveCapacity())
                .fullDayCapacityHours(s.getFullDayCapacityHours())
                .jobCardSerialPrefix(s.getJobCardSerialPrefix())
                .serviceReportingTime(s.getServiceReportingTime())
                .expressReportingTime(s.getExpressReportingTime())
                .advanceEnabled(s.getAdvanceEnabled())
                .advanceAmount(s.getAdvanceAmount())
                .serviceSettings(services)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private MechanicServiceSettingResponse toServiceResponse(MechanicServiceSetting e) {
        return MechanicServiceSettingResponse.builder()
                .id(e.getId())
                .serviceName(e.getServiceName())
                .durationMinutes(e.getDurationMinutes())
                .maxSlotsPerDay(e.getMaxSlotsPerDay())
                .isExpressEligible(e.getIsExpressEligible())
                .isActive(e.getIsActive())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private User resolveUser(Long mechanicId) {
        return settingsRepository.findByMechanicId(mechanicId)
                .map(MechanicSettings::getMechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found: " + mechanicId));
    }
}
