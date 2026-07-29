package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.MechanicConfigurationSettingsRequest;
import com.alvexo.bookingapp.dto.request.MechanicMasterEntryRequest;
import com.alvexo.bookingapp.dto.response.MechanicConfigurationSettingsResponse;
import com.alvexo.bookingapp.dto.response.MechanicMasterEntryResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.MechanicMasterEntry;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.bookingapp.repository.MechanicMasterEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Backs the "Configuration Settings" section of Workshop Settings — a
 * separate row from MechanicSettings since these fields don't affect
 * capacity/booking logic and are edited independently (spec §7 / A4).
 */
@Service
public class MechanicConfigurationSettingsService {

    private static final int MAX_REMINDER_CHANGES_PER_YEAR = 2;

    private final MechanicConfigurationSettingsRepository configRepository;
    private final MechanicMasterEntryRepository masterEntryRepository;
    private final MechanicSettingsAuditLogService auditLogService;

    public MechanicConfigurationSettingsService(MechanicConfigurationSettingsRepository configRepository,
                                                 MechanicMasterEntryRepository masterEntryRepository,
                                                 MechanicSettingsAuditLogService auditLogService) {
        this.configRepository = configRepository;
        this.masterEntryRepository = masterEntryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MechanicConfigurationSettingsResponse saveConfiguration(User mechanic,
                                                                     MechanicConfigurationSettingsRequest request) {
        validateRole(mechanic);

        MechanicConfigurationSettings config = configRepository.findByMechanic(mechanic)
                .orElse(MechanicConfigurationSettings.builder().mechanic(mechanic).build());

        boolean isNew = config.getId() == null;
        MechanicConfigurationSettings before = isNew ? null : copyOf(config);

        applyReminderChangeCap(config, request);

        config.setJobCardNumberStartingSequence(request.getJobCardNumberStartingSequence());
        config.setJobCardNumberFormat(request.getJobCardNumberFormat());
        config.setJobCardNumberResetFrequency(request.getJobCardNumberResetFrequency());
        config.setJobCardNumberPrefix(request.getJobCardNumberPrefix());
        config.setJobCardNumberSuffix(request.getJobCardNumberSuffix());
        config.setRescheduleLimit(request.getRescheduleLimit());
        config.setRescheduleCutoffTime(request.getRescheduleCutoffTime());
        config.setPickupDropFacilityEnabled(request.getPickupDropFacilityEnabled());
        config.setAutoConfirmOtherState(request.getAutoConfirmOtherState());
        config.setRepairsRequireAdvance(request.getRepairsRequireAdvance());
        config.setServiceDueIntervalDays(request.getServiceDueIntervalDays());
        config.setSecondReminderIntervalDays(request.getSecondReminderIntervalDays());

        config = configRepository.save(config);

        if (before != null) {
            logConfigurationChanges(mechanic, before, config);
        }

        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public MechanicConfigurationSettingsResponse getConfiguration(User mechanic) {
        MechanicConfigurationSettings config = configRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration settings not set up yet"));
        return toResponse(config);
    }

    /**
     * Enforces the 2x/year cap on changing the reminder intervals (spec §7 8b).
     * Resets the counter automatically when the calendar year rolls over.
     */
    private void applyReminderChangeCap(MechanicConfigurationSettings config,
                                         MechanicConfigurationSettingsRequest request) {
        int currentYear = LocalDate.now().getYear();
        if (config.getId() == null) {
            // First-time creation is not a "change" — nothing to cap.
            config.setReminderChangesYear(currentYear);
            return;
        }

        boolean intervalsChanged =
                !Objects.equals(config.getServiceDueIntervalDays(), request.getServiceDueIntervalDays())
                || !Objects.equals(config.getSecondReminderIntervalDays(), request.getSecondReminderIntervalDays());

        if (!intervalsChanged) {
            return;
        }

        if (config.getReminderChangesYear() == null || config.getReminderChangesYear() != currentYear) {
            config.setReminderChangesYear(currentYear);
            config.setReminderChangesThisYear(0);
        }

        if (config.getReminderChangesThisYear() >= MAX_REMINDER_CHANGES_PER_YEAR) {
            throw new BadRequestException(
                    "Service reminder intervals can only be changed " + MAX_REMINDER_CHANGES_PER_YEAR
                            + " times per year. Limit reached for " + currentYear + ".");
        }

        config.setReminderChangesThisYear(config.getReminderChangesThisYear() + 1);
    }

    // ── Mechanic Master CRUD ───────────────────────────────────────────────

    @Transactional
    public MechanicMasterEntryResponse addMechanicMasterEntry(User mechanic, MechanicMasterEntryRequest request) {
        validateRole(mechanic);
        MechanicMasterEntry entry = MechanicMasterEntry.builder()
                .mechanic(mechanic)
                .name(request.getName())
                .phone(request.getPhone())
                .active(request.getActive() != null ? request.getActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        return toResponse(masterEntryRepository.save(entry));
    }

    @Transactional
    public MechanicMasterEntryResponse updateMechanicMasterEntry(User mechanic, Long entryId,
                                                                   MechanicMasterEntryRequest request) {
        validateRole(mechanic);
        MechanicMasterEntry entry = masterEntryRepository.findByIdAndMechanic(entryId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic master entry not found"));

        entry.setName(request.getName());
        entry.setPhone(request.getPhone());
        if (request.getActive() != null) entry.setActive(request.getActive());
        if (request.getDisplayOrder() != null) entry.setDisplayOrder(request.getDisplayOrder());

        return toResponse(masterEntryRepository.save(entry));
    }

    @Transactional
    public void deleteMechanicMasterEntry(User mechanic, Long entryId) {
        validateRole(mechanic);
        MechanicMasterEntry entry = masterEntryRepository.findByIdAndMechanic(entryId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic master entry not found"));
        masterEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<MechanicMasterEntryResponse> getMechanicMasterEntries(Long mechanicId) {
        return masterEntryRepository.findByMechanicIdOrderByDisplayOrderAsc(mechanicId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Audit ───────────────────────────────────────────────────────────────

    private MechanicConfigurationSettings copyOf(MechanicConfigurationSettings c) {
        return MechanicConfigurationSettings.builder()
                .jobCardNumberStartingSequence(c.getJobCardNumberStartingSequence())
                .jobCardNumberFormat(c.getJobCardNumberFormat())
                .jobCardNumberResetFrequency(c.getJobCardNumberResetFrequency())
                .jobCardNumberPrefix(c.getJobCardNumberPrefix())
                .jobCardNumberSuffix(c.getJobCardNumberSuffix())
                .rescheduleLimit(c.getRescheduleLimit())
                .rescheduleCutoffTime(c.getRescheduleCutoffTime())
                .pickupDropFacilityEnabled(c.getPickupDropFacilityEnabled())
                .autoConfirmOtherState(c.getAutoConfirmOtherState())
                .repairsRequireAdvance(c.getRepairsRequireAdvance())
                .serviceDueIntervalDays(c.getServiceDueIntervalDays())
                .secondReminderIntervalDays(c.getSecondReminderIntervalDays())
                .build();
    }

    private void logConfigurationChanges(User mechanic, MechanicConfigurationSettings before,
                                          MechanicConfigurationSettings after) {
        String entityType = "CONFIGURATION_SETTINGS";
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "jobCardNumberStartingSequence", before.getJobCardNumberStartingSequence(), after.getJobCardNumberStartingSequence());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "jobCardNumberFormat", before.getJobCardNumberFormat(), after.getJobCardNumberFormat());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "jobCardNumberResetFrequency", before.getJobCardNumberResetFrequency(), after.getJobCardNumberResetFrequency());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "jobCardNumberPrefix", before.getJobCardNumberPrefix(), after.getJobCardNumberPrefix());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "jobCardNumberSuffix", before.getJobCardNumberSuffix(), after.getJobCardNumberSuffix());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "rescheduleLimit", before.getRescheduleLimit(), after.getRescheduleLimit());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "rescheduleCutoffTime", before.getRescheduleCutoffTime(), after.getRescheduleCutoffTime());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "pickupDropFacilityEnabled", before.getPickupDropFacilityEnabled(), after.getPickupDropFacilityEnabled());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "autoConfirmOtherState", before.getAutoConfirmOtherState(), after.getAutoConfirmOtherState());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "repairsRequireAdvance", before.getRepairsRequireAdvance(), after.getRepairsRequireAdvance());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "serviceDueIntervalDays", before.getServiceDueIntervalDays(), after.getServiceDueIntervalDays());
        auditLogService.logIfChanged(mechanic, mechanic, entityType, "secondReminderIntervalDays", before.getSecondReminderIntervalDays(), after.getSecondReminderIntervalDays());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage configuration settings");
        }
    }

    private MechanicConfigurationSettingsResponse toResponse(MechanicConfigurationSettings c) {
        int remaining = MAX_REMINDER_CHANGES_PER_YEAR
                - (c.getReminderChangesYear() != null && c.getReminderChangesYear() == LocalDate.now().getYear()
                        ? c.getReminderChangesThisYear() : 0);

        return MechanicConfigurationSettingsResponse.builder()
                .id(c.getId())
                .mechanicId(c.getMechanic().getId())
                .jobCardNumberStartingSequence(c.getJobCardNumberStartingSequence())
                .jobCardNumberFormat(c.getJobCardNumberFormat())
                .jobCardNumberResetFrequency(c.getJobCardNumberResetFrequency())
                .jobCardNumberPrefix(c.getJobCardNumberPrefix())
                .jobCardNumberSuffix(c.getJobCardNumberSuffix())
                .rescheduleLimit(c.getRescheduleLimit())
                .rescheduleCutoffTime(c.getRescheduleCutoffTime())
                .pickupDropFacilityEnabled(c.getPickupDropFacilityEnabled())
                .autoConfirmOtherState(c.getAutoConfirmOtherState())
                .repairsRequireAdvance(c.getRepairsRequireAdvance())
                .serviceDueIntervalDays(c.getServiceDueIntervalDays())
                .secondReminderIntervalDays(c.getSecondReminderIntervalDays())
                .reminderChangesThisYear(c.getReminderChangesThisYear())
                .reminderChangesRemainingThisYear(Math.max(remaining, 0))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private MechanicMasterEntryResponse toResponse(MechanicMasterEntry e) {
        return MechanicMasterEntryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .phone(e.getPhone())
                .active(e.getActive())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
