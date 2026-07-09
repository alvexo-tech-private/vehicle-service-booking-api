package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.MechanicServiceSettingRequest;
import com.alvexo.bookingapp.dto.request.MechanicSettingsRequest;
import com.alvexo.bookingapp.dto.request.MechanicServiceSlotsRequest;
import com.alvexo.bookingapp.dto.request.ServiceSlotRequest;
import com.alvexo.bookingapp.dto.response.MechanicServiceSettingResponse;
import com.alvexo.bookingapp.dto.response.MechanicSettingsResponse;
import com.alvexo.bookingapp.dto.response.MechanicServiceSlotResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSlotRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MechanicSettingsService {

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository serviceSlotRepository;

    public MechanicSettingsService(MechanicSettingsRepository settingsRepository,
                                   MechanicServiceSettingRepository serviceSettingRepository,
                                   MechanicServiceSlotRepository serviceSlotRepository) {
        this.settingsRepository = settingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.serviceSlotRepository = serviceSlotRepository;
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
        entity.setCategory(request.getCategory());
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
        if (r.getJobCardType() == JobCardType.AUTO && Boolean.TRUE.equals(r.getReserveForSlots())) {
            throw new BadRequestException("reserveForSlots must be false when jobCardType is AUTO");
        }
        if (Boolean.TRUE.equals(r.getReserveCapacity())) {
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
        if (Boolean.TRUE.equals(r.getAutoAllocationEnabled()) && r.getAutoAllocationCapacityHours() == null) {
            throw new BadRequestException("autoAllocationCapacityHours is required when autoAllocationEnabled is true");
        }
    }

    private void applyRequest(MechanicSettings s, MechanicSettingsRequest r) {
        s.setJobCardType(r.getJobCardType());
        s.setMaxVehiclesPerDay(r.getMaxVehiclesPerDay());
        // reserveCapacity and reserveForSlots are mutually exclusive per jobCardType;
        // normalize the unused flag to false rather than trusting the client value.
        s.setReserveCapacity(r.getJobCardType() == JobCardType.AUTO && Boolean.TRUE.equals(r.getReserveCapacity()));
        s.setReserveForSlots(r.getJobCardType() == JobCardType.MECHANIC && Boolean.TRUE.equals(r.getReserveForSlots()));
        s.setClassification(deriveClassification(s.getJobCardType(), s.getReserveCapacity(), s.getReserveForSlots()));
        s.setFullDayCapacityHours(r.getFullDayCapacityHours());
        s.setJobCardSerialPrefix(r.getJobCardSerialPrefix());
        s.setServiceReportingTime(r.getServiceReportingTime());
        s.setExpressReportingTime(r.getExpressReportingTime());
        s.setAdvanceEnabled(r.getAdvanceEnabled());
        s.setAdvanceAmount(r.getAdvanceAmount());
        s.setAutoAllocationEnabled(r.getAutoAllocationEnabled());
        s.setAutoAllocationCapacityHours(r.getAutoAllocationCapacityHours());
    }

    /**
     * Level 1 -> TYPE_1 (AUTO,     reserveCapacity=false)
     * Level 2 -> TYPE_2 (AUTO,     reserveCapacity=true)
     * Level 3 -> TYPE_3 (MECHANIC, reserveForSlots=false)
     * Level 4 -> TYPE_4 (MECHANIC, reserveForSlots=true)
     */
    private WorkshopClassification deriveClassification(JobCardType jobCardType,
                                                          Boolean reserveCapacity,
                                                          Boolean reserveForSlots) {
        if (jobCardType == JobCardType.AUTO) {
            return Boolean.TRUE.equals(reserveCapacity) ? WorkshopClassification.TYPE_2 : WorkshopClassification.TYPE_1;
        }
        return Boolean.TRUE.equals(reserveForSlots) ? WorkshopClassification.TYPE_4 : WorkshopClassification.TYPE_3;
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
                .category(r.getCategory())
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
                .jobCardType(s.getJobCardType())
                .classification(s.getClassification())
                .maxVehiclesPerDay(s.getMaxVehiclesPerDay())
                .reserveCapacity(s.getReserveCapacity())
                .reserveForSlots(s.getReserveForSlots())
                .fullDayCapacityHours(s.getFullDayCapacityHours())
                .jobCardSerialPrefix(s.getJobCardSerialPrefix())
                .serviceReportingTime(s.getServiceReportingTime())
                .expressReportingTime(s.getExpressReportingTime())
                .advanceEnabled(s.getAdvanceEnabled())
                .advanceAmount(s.getAdvanceAmount())
                .autoAllocationEnabled(s.getAutoAllocationEnabled())
                .autoAllocationCapacityHours(s.getAutoAllocationCapacityHours())
                .serviceSettings(services)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private MechanicServiceSettingResponse toServiceResponse(MechanicServiceSetting e) {
        return MechanicServiceSettingResponse.builder()
                .id(e.getId())
                .serviceName(e.getServiceName())
                .category(e.getCategory())
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

    // ── Service slots (Slot 1 / Slot 2 — Level 4) ─────────────────────────────

    /**
     * Create-or-update semantics: each item in the request is upserted by
     * (mechanic, slotNumber). Slots omitted from the request are left untouched.
     */
    @Transactional
    public List<MechanicServiceSlotResponse> saveServiceSlots(User mechanic, MechanicServiceSlotsRequest request) {
        validateRole(mechanic);

        if (!settingsRepository.existsByMechanic(mechanic)) {
            throw new BadRequestException("Mechanic settings must be created before saving service slots");
        }

        Set<Integer> seen = new HashSet<>();
        for (ServiceSlotRequest item : request.getSlots()) {
            if (!seen.add(item.getSlotNumber())) {
                throw new BadRequestException("Duplicate slotNumber in request: " + item.getSlotNumber());
            }
            if (Boolean.TRUE.equals(item.getEnabled() == null ? Boolean.TRUE : item.getEnabled())
                    && item.getSlotTime() == null) {
                throw new BadRequestException("slotTime is required for slot " + item.getSlotNumber() + " when enabled");
            }
        }

        List<MechanicServiceSlot> saved = request.getSlots().stream()
                .map(item -> {
                    MechanicServiceSlot slot = serviceSlotRepository
                            .findByMechanicAndSlotNumber(mechanic, item.getSlotNumber())
                            .orElse(MechanicServiceSlot.builder()
                                    .mechanic(mechanic)
                                    .slotNumber(item.getSlotNumber())
                                    .build());
                    slot.setSlotTime(item.getSlotTime());
                    slot.setRepairQty(item.getRepairQty());
                    slot.setEnabled(item.getEnabled() == null ? Boolean.TRUE : item.getEnabled());
                    return serviceSlotRepository.save(slot);
                })
                .collect(Collectors.toList());

        return saved.stream()
                .sorted(Comparator.comparing(MechanicServiceSlot::getSlotNumber))
                .map(this::toSlotResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MechanicServiceSlotResponse> getServiceSlots(Long mechanicId) {
        if (!settingsRepository.existsByMechanicId(mechanicId)) {
            throw new ResourceNotFoundException("Settings not found for mechanic " + mechanicId);
        }
        return serviceSlotRepository.findByMechanicIdOrderBySlotNumberAsc(mechanicId)
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    private MechanicServiceSlotResponse toSlotResponse(MechanicServiceSlot s) {
        return MechanicServiceSlotResponse.builder()
                .id(s.getId())
                .slotNumber(s.getSlotNumber())
                .slotTime(s.getSlotTime())
                .repairQty(s.getRepairQty())
                .enabled(s.getEnabled())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
