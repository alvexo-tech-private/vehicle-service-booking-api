package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.*;
import com.alvexo.bookingapp.dto.response.*;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MechanicSettingsService {

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository slotRepository;
    private final MechanicTechnicianCapacityRepository technicianRepository;
    private final DailyQuotaOverrideRepository overrideRepository;

    public MechanicSettingsService(MechanicSettingsRepository settingsRepository,
                                   MechanicServiceSettingRepository serviceSettingRepository,
                                   MechanicServiceSlotRepository slotRepository,
                                   MechanicTechnicianCapacityRepository technicianRepository,
                                   DailyQuotaOverrideRepository overrideRepository) {
        this.settingsRepository = settingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.slotRepository = slotRepository;
        this.technicianRepository = technicianRepository;
        this.overrideRepository = overrideRepository;
    }

    // ── Settings (create / update / get) ────────────────────────────────────

    @Transactional
    public MechanicSettingsResponse createSettings(User mechanic, MechanicSettingsRequest request) {
        validateRole(mechanic);
        validateSettingsRequest(request);

        if (settingsRepository.existsByMechanic(mechanic)) {
            throw new BadRequestException("Settings already exist. Use PUT to update.");
        }

        MechanicSettings settings = MechanicSettings.builder().mechanic(mechanic).build();
        applyRequest(settings, request);
        settings = settingsRepository.save(settings);

        if (request.getServiceSettings() != null) {
            replaceServiceSettings(mechanic, request.getServiceSettings());
        }

        return buildResponse(settings);
    }

    @Transactional
    public MechanicSettingsResponse updateSettings(User mechanic, MechanicSettingsRequest request) {
        validateRole(mechanic);
        validateSettingsRequest(request);

        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Settings not found. Use POST to create first."));

        applyRequest(settings, request);
        settings = settingsRepository.save(settings);

        if (request.getServiceSettings() != null) {
            replaceServiceSettings(mechanic, request.getServiceSettings());
        }

        return buildResponse(settings);
    }

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

    // ── Service settings CRUD ────────────────────────────────────────────────

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
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
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
        return serviceSettingRepository
                .findByMechanicAndIsActiveTrueOrderByDisplayOrderAsc(resolveUser(mechanicId))
                .stream().map(this::toServiceResponse).collect(Collectors.toList());
    }

    // ── Service Slot CRUD (TYPE_4) ───────────────────────────────────────────

    @Transactional
    public MechanicServiceSlotResponse addServiceSlot(User mechanic, MechanicServiceSlotRequest request) {
        validateRole(mechanic);
        MechanicSettings settings = getOrThrowSettings(mechanic);
        validateSlotRequest(request);

        MechanicServiceSlot slot = MechanicServiceSlot.builder()
                .mechanicSettings(settings)
                .slotNumber(request.getSlotNumber())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .restrictedCategory(request.getRestrictedCategory())
                .maxVehicleQty(request.getMaxVehicleQty())
                .autoAllocationQty(request.getAutoAllocationQty() != null ? request.getAutoAllocationQty() : 0)
                .applicableDays(request.getApplicableDays())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .build();

        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public MechanicServiceSlotResponse updateServiceSlot(User mechanic, Long slotId,
                                                          MechanicServiceSlotRequest request) {
        validateRole(mechanic);
        MechanicServiceSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Service slot not found"));
        verifySlotOwnership(slot, mechanic);
        validateSlotRequest(request);

        slot.setSlotNumber(request.getSlotNumber());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setRestrictedCategory(request.getRestrictedCategory());
        slot.setMaxVehicleQty(request.getMaxVehicleQty());
        if (request.getAutoAllocationQty() != null) slot.setAutoAllocationQty(request.getAutoAllocationQty());
        if (request.getApplicableDays() != null) slot.setApplicableDays(request.getApplicableDays());
        if (request.getIsEnabled() != null) slot.setIsEnabled(request.getIsEnabled());

        return toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public void deleteServiceSlot(User mechanic, Long slotId) {
        validateRole(mechanic);
        MechanicServiceSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Service slot not found"));
        verifySlotOwnership(slot, mechanic);
        slotRepository.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<MechanicServiceSlotResponse> getServiceSlots(Long mechanicId) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found for mechanic " + mechanicId));
        return slotRepository.findByMechanicSettingsAndIsEnabledTrue(settings)
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    // ── Technician Capacity CRUD (TYPE_3/4) ──────────────────────────────────

    @Transactional
    public MechanicTechnicianCapacityResponse addTechnician(User mechanic,
                                                             MechanicTechnicianCapacityRequest request) {
        validateRole(mechanic);
        MechanicSettings settings = getOrThrowSettings(mechanic);

        MechanicTechnicianCapacity tech = MechanicTechnicianCapacity.builder()
                .mechanicSettings(settings)
                .technicianName(request.getTechnicianName())
                .reservedHours(request.getReservedHours())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toTechnicianResponse(technicianRepository.save(tech));
    }

    @Transactional
    public MechanicTechnicianCapacityResponse updateTechnician(User mechanic, Long techId,
                                                                MechanicTechnicianCapacityRequest request) {
        validateRole(mechanic);
        MechanicTechnicianCapacity tech = technicianRepository.findById(techId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        verifyTechnicianOwnership(tech, mechanic);

        tech.setTechnicianName(request.getTechnicianName());
        tech.setReservedHours(request.getReservedHours());
        if (request.getIsActive() != null) tech.setIsActive(request.getIsActive());

        return toTechnicianResponse(technicianRepository.save(tech));
    }

    @Transactional
    public void deleteTechnician(User mechanic, Long techId) {
        validateRole(mechanic);
        MechanicTechnicianCapacity tech = technicianRepository.findById(techId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        verifyTechnicianOwnership(tech, mechanic);
        technicianRepository.delete(tech);
    }

    @Transactional(readOnly = true)
    public List<MechanicTechnicianCapacityResponse> getTechnicians(User mechanic) {
        validateRole(mechanic);
        MechanicSettings settings = getOrThrowSettings(mechanic);
        return technicianRepository.findByMechanicSettings(settings)
                .stream().map(this::toTechnicianResponse).collect(Collectors.toList());
    }

    // ── Daily Quota Override CRUD ────────────────────────────────────────────

    @Transactional
    public DailyQuotaOverrideResponse addOverride(User mechanic, DailyQuotaOverrideRequest request) {
        validateRole(mechanic);
        MechanicSettings settings = getOrThrowSettings(mechanic);

        if (request.getOverrideMaxVehicles() == null && request.getOverrideCapacityHours() == null) {
            throw new BadRequestException("At least one override value (maxVehicles or capacityHours) is required");
        }

        DailyQuotaOverride override = DailyQuotaOverride.builder()
                .mechanicSettings(settings)
                .overrideDate(request.getOverrideDate())
                .overrideMaxVehicles(request.getOverrideMaxVehicles())
                .overrideCapacityHours(request.getOverrideCapacityHours())
                .reason(request.getReason())
                .isActive(true)
                .build();

        return toOverrideResponse(overrideRepository.save(override));
    }

    @Transactional
    public DailyQuotaOverrideResponse updateOverride(User mechanic, Long overrideId,
                                                      DailyQuotaOverrideRequest request) {
        validateRole(mechanic);
        DailyQuotaOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new ResourceNotFoundException("Override not found"));
        verifyOverrideOwnership(override, mechanic);

        override.setOverrideDate(request.getOverrideDate());
        override.setOverrideMaxVehicles(request.getOverrideMaxVehicles());
        override.setOverrideCapacityHours(request.getOverrideCapacityHours());
        if (request.getReason() != null) override.setReason(request.getReason());

        return toOverrideResponse(overrideRepository.save(override));
    }

    @Transactional
    public void deleteOverride(User mechanic, Long overrideId) {
        validateRole(mechanic);
        DailyQuotaOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new ResourceNotFoundException("Override not found"));
        verifyOverrideOwnership(override, mechanic);
        overrideRepository.delete(override);
    }

    @Transactional(readOnly = true)
    public List<DailyQuotaOverrideResponse> getOverrides(User mechanic) {
        validateRole(mechanic);
        MechanicSettings settings = getOrThrowSettings(mechanic);
        return overrideRepository.findByMechanicSettingsOrderByOverrideDateDesc(settings)
                .stream().map(this::toOverrideResponse).collect(Collectors.toList());
    }

    // ── Effective capacity resolution (used by BookingService) ───────────────

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveCapacityHours(MechanicSettings settings, LocalDate date) {
        DailyQuotaOverride override = overrideRepository
                .findByMechanicSettingsAndOverrideDateAndIsActiveTrue(settings, date)
                .orElse(null);

        if (override != null && override.getOverrideCapacityHours() != null) {
            return override.getOverrideCapacityHours();
        }

        BigDecimal technicianHours = technicianRepository.sumActiveReservedHours(settings);
        if (technicianHours.compareTo(BigDecimal.ZERO) > 0) {
            return technicianHours;
        }

        return settings.getFullDayCapacityHours();
    }

    @Transactional(readOnly = true)
    public Integer getEffectiveMaxVehicles(MechanicSettings settings, LocalDate date) {
        DailyQuotaOverride override = overrideRepository
                .findByMechanicSettingsAndOverrideDateAndIsActiveTrue(settings, date)
                .orElse(null);

        if (override != null && override.getOverrideMaxVehicles() != null) {
            return override.getOverrideMaxVehicles();
        }

        return settings.getMaxVehiclesPerDay();
    }

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

    // ── Private: validation ──────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage settings");
        }
    }

    private void validateSettingsRequest(MechanicSettingsRequest r) {
        JobCardType type = r.getJobCardType();

        if (type == JobCardType.TYPE_3 || type == JobCardType.TYPE_4) {
            if (r.getFullDayCapacityHours() == null) {
                throw new BadRequestException("fullDayCapacityHours is required for " + type);
            }
            if (r.getAutoAllocationCapacityHours() == null) {
                throw new BadRequestException("autoAllocationCapacityHours is required for " + type);
            }
            if (r.getAutoAllocationCapacityHours().compareTo(r.getFullDayCapacityHours()) > 0) {
                throw new BadRequestException("autoAllocationCapacityHours cannot exceed fullDayCapacityHours");
            }
            if (r.getExpressReportingTime() == null) {
                throw new BadRequestException("expressReportingTime is required for " + type);
            }
        }

        if (Boolean.TRUE.equals(r.getReserveCapacity())) {
            if (r.getFullDayCapacityHours() == null) {
                throw new BadRequestException("fullDayCapacityHours is required when reserveCapacity is true");
            }
        }

        if (Boolean.TRUE.equals(r.getAdvanceEnabled()) && r.getAdvanceAmount() == null) {
            throw new BadRequestException("advanceAmount is required when advanceEnabled is true");
        }
    }

    private void validateSlotRequest(MechanicServiceSlotRequest r) {
        if (!r.getStartTime().isBefore(r.getEndTime())) {
            throw new BadRequestException("Slot startTime must be before endTime");
        }
        if (r.getAutoAllocationQty() != null && r.getAutoAllocationQty() > r.getMaxVehicleQty()) {
            throw new BadRequestException("autoAllocationQty cannot exceed maxVehicleQty");
        }
        if (r.getApplicableDays() != null && !r.getApplicableDays().isBlank()) {
            for (String day : r.getApplicableDays().split(",")) {
                try {
                    com.alvexo.bookingapp.model.DayOfWeek.valueOf(day.trim());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid day in applicableDays: " + day.trim()
                            + ". Valid values: MON, TUE, WED, THU, FRI, SAT, SUN");
                }
            }
        }
    }

    // ── Private: ownership verification ──────────────────────────────────────

    private void verifySlotOwnership(MechanicServiceSlot slot, User mechanic) {
        if (!slot.getMechanicSettings().getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only manage your own service slots");
        }
    }

    private void verifyTechnicianOwnership(MechanicTechnicianCapacity tech, User mechanic) {
        if (!tech.getMechanicSettings().getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only manage your own technicians");
        }
    }

    private void verifyOverrideOwnership(DailyQuotaOverride override, User mechanic) {
        if (!override.getMechanicSettings().getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You can only manage your own overrides");
        }
    }

    // ── Private: entity helpers ──────────────────────────────────────────────

    private MechanicSettings getOrThrowSettings(User mechanic) {
        return settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured yet. Save settings first."));
    }

    private void applyRequest(MechanicSettings s, MechanicSettingsRequest r) {
        s.setJobCardType(r.getJobCardType());
        s.setClassification(r.getClassification());
        s.setMaxVehiclesPerDay(r.getMaxVehiclesPerDay());
        s.setReserveCapacity(r.getReserveCapacity());
        s.setFullDayCapacityHours(r.getFullDayCapacityHours());
        s.setJobCardSerialPrefix(r.getJobCardSerialPrefix());
        s.setServiceReportingTime(r.getServiceReportingTime());
        s.setExpressReportingTime(r.getExpressReportingTime());
        s.setAdvanceEnabled(r.getAdvanceEnabled());
        s.setAdvanceAmount(r.getAdvanceAmount());
        s.setTotalDailyCapacityHours(r.getTotalDailyCapacityHours());
        s.setAutoAllocationEnabled(r.getAutoAllocationEnabled() != null ? r.getAutoAllocationEnabled() : false);
        s.setAutoAllocationCapacityHours(r.getAutoAllocationCapacityHours());
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
                .category(r.getCategory() != null ? r.getCategory() : ServiceCategory.GENERAL)
                .isExpressEligible(r.getIsExpressEligible() != null ? r.getIsExpressEligible() : false)
                .isActive(r.getIsActive() != null ? r.getIsActive() : true)
                .displayOrder(r.getDisplayOrder() != null ? r.getDisplayOrder() : 0)
                .build();
    }

    // ── Private: response builders ───────────────────────────────────────────

    private MechanicSettingsResponse buildResponse(MechanicSettings s) {
        List<MechanicServiceSettingResponse> services = serviceSettingRepository
                .findByMechanicOrderByDisplayOrderAsc(s.getMechanic())
                .stream().map(this::toServiceResponse).collect(Collectors.toList());

        List<MechanicTechnicianCapacityResponse> technicians = technicianRepository
                .findByMechanicSettings(s)
                .stream().map(this::toTechnicianResponse).collect(Collectors.toList());

        BigDecimal technicianHours = technicianRepository.sumActiveReservedHours(s);
        BigDecimal effectiveCapacity = technicianHours.compareTo(BigDecimal.ZERO) > 0
                ? technicianHours
                : s.getFullDayCapacityHours();

        BigDecimal reservedCapacity = null;
        if (effectiveCapacity != null && s.getAutoAllocationCapacityHours() != null) {
            reservedCapacity = effectiveCapacity.subtract(s.getAutoAllocationCapacityHours());
        }

        return MechanicSettingsResponse.builder()
                .id(s.getId())
                .mechanicId(s.getMechanic().getId())
                .mechanicName(s.getMechanic().getFirstName() + " " + s.getMechanic().getLastName())
                .jobCardType(s.getJobCardType())
                .classification(s.getClassification())
                .maxVehiclesPerDay(s.getMaxVehiclesPerDay())
                .reserveCapacity(s.getReserveCapacity())
                .fullDayCapacityHours(s.getFullDayCapacityHours())
                .jobCardSerialPrefix(s.getJobCardSerialPrefix())
                .serviceReportingTime(s.getServiceReportingTime())
                .expressReportingTime(s.getExpressReportingTime())
                .advanceEnabled(s.getAdvanceEnabled())
                .advanceAmount(s.getAdvanceAmount())
                .totalDailyCapacityHours(s.getTotalDailyCapacityHours())
                .autoAllocationEnabled(s.getAutoAllocationEnabled())
                .autoAllocationCapacityHours(s.getAutoAllocationCapacityHours())
                .effectiveCapacityHours(effectiveCapacity)
                .reservedCapacityHours(reservedCapacity)
                .serviceSettings(services)
                .technicianCapacities(technicians)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private MechanicServiceSettingResponse toServiceResponse(MechanicServiceSetting e) {
        return MechanicServiceSettingResponse.builder()
                .id(e.getId())
                .category(e.getCategory())
                .serviceName(e.getServiceName())
                .durationMinutes(e.getDurationMinutes())
                .maxSlotsPerDay(e.getMaxSlotsPerDay())
                .isExpressEligible(e.getIsExpressEligible())
                .isActive(e.getIsActive())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MechanicServiceSlotResponse toSlotResponse(MechanicServiceSlot e) {
        return MechanicServiceSlotResponse.builder()
                .id(e.getId())
                .slotNumber(e.getSlotNumber())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .restrictedCategory(e.getRestrictedCategory())
                .maxVehicleQty(e.getMaxVehicleQty())
                .autoAllocationQty(e.getAutoAllocationQty())
                .manualReviewQty(e.getManualReviewQty())
                .applicableDays(e.getApplicableDays())
                .isEnabled(e.getIsEnabled())
                .build();
    }

    private MechanicTechnicianCapacityResponse toTechnicianResponse(MechanicTechnicianCapacity e) {
        return MechanicTechnicianCapacityResponse.builder()
                .id(e.getId())
                .technicianName(e.getTechnicianName())
                .reservedHours(e.getReservedHours())
                .isActive(e.getIsActive())
                .build();
    }

    private DailyQuotaOverrideResponse toOverrideResponse(DailyQuotaOverride e) {
        return DailyQuotaOverrideResponse.builder()
                .id(e.getId())
                .overrideDate(e.getOverrideDate())
                .overrideMaxVehicles(e.getOverrideMaxVehicles())
                .overrideCapacityHours(e.getOverrideCapacityHours())
                .reason(e.getReason())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private User resolveUser(Long mechanicId) {
        return settingsRepository.findByMechanicId(mechanicId)
                .map(MechanicSettings::getMechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found: " + mechanicId));
    }
}
