package com.alvexo.bookingapp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.MechanicConfigurationSettingsRequest;
import com.alvexo.bookingapp.dto.request.MechanicSettingsRequest;
import com.alvexo.bookingapp.dto.request.RestorePointRequest;
import com.alvexo.bookingapp.dto.response.RestorePointSummaryResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.MechanicSettingsRestorePoint;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRestorePointRepository;

import lombok.RequiredArgsConstructor;

/**
 * Settings Restore Points (WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md §1.2) —
 * named snapshots of a workshop's current capacity + configuration settings that can
 * be re-applied later. Snapshots are taken from live server state (not the request
 * body) so a restore always reflects what was actually saved at that point in time.
 */
@Service
@RequiredArgsConstructor
public class MechanicSettingsRestorePointService {

    private final MechanicSettingsRestorePointRepository restorePointRepository;
    private final MechanicSettingsRepository settingsRepository;
    private final MechanicConfigurationSettingsRepository configRepository;
    private final MechanicSettingsService settingsService;
    private final MechanicConfigurationSettingsService configService;

    @Transactional
    public RestorePointSummaryResponse save(User mechanic, RestorePointRequest request) {
        validateRole(mechanic);

        MechanicSettingsRequest settingsSnapshot = settingsRepository.findByMechanic(mechanic)
                .map(this::toSnapshot).orElse(null);
        MechanicConfigurationSettingsRequest configSnapshot = configRepository.findByMechanic(mechanic)
                .map(this::toSnapshot).orElse(null);

        if (settingsSnapshot == null && configSnapshot == null) {
            throw new BadRequestException("No settings configured yet — nothing to snapshot");
        }

        MechanicSettingsRestorePoint saved = restorePointRepository.save(MechanicSettingsRestorePoint.builder()
                .mechanic(mechanic)
                .name(request.getName())
                .settingsSnapshot(settingsSnapshot)
                .configurationSnapshot(configSnapshot)
                .build());

        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<RestorePointSummaryResponse> list(User mechanic) {
        validateRole(mechanic);
        return restorePointRepository.findByMechanicOrderBySavedAtDesc(mechanic).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void restore(User mechanic, Long restorePointId) {
        validateRole(mechanic);
        MechanicSettingsRestorePoint restorePoint = restorePointRepository
                .findByIdAndMechanic(restorePointId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Restore point not found"));

        if (restorePoint.getSettingsSnapshot() != null) {
            settingsService.saveSettings(mechanic, restorePoint.getSettingsSnapshot());
        }
        if (restorePoint.getConfigurationSnapshot() != null) {
            configService.saveConfiguration(mechanic, restorePoint.getConfigurationSnapshot());
        }
    }

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can manage settings restore points");
        }
    }

    private RestorePointSummaryResponse toSummary(MechanicSettingsRestorePoint p) {
        return new RestorePointSummaryResponse(p.getId(), p.getName(), p.getSavedAt());
    }

    private MechanicSettingsRequest toSnapshot(MechanicSettings s) {
        MechanicSettingsRequest r = new MechanicSettingsRequest();
        r.setJobCardType(s.getJobCardType());
        r.setMaxVehiclesPerDay(s.getMaxVehiclesPerDay());
        r.setReserveCapacity(s.getReserveCapacity());
        r.setReserveForSlots(s.getReserveForSlots());
        r.setFullDayCapacityHours(s.getFullDayCapacityHours());
        r.setJobCardSerialPrefix(s.getJobCardSerialPrefix());
        r.setServiceReportingTime(s.getServiceReportingTime());
        r.setExpressReportingTime(s.getExpressReportingTime());
        r.setAdvanceEnabled(s.getAdvanceEnabled());
        r.setAdvanceAmount(s.getAdvanceAmount());
        r.setAutoAllocationEnabled(s.getAutoAllocationEnabled());
        r.setAutoAllocationCapacityHours(s.getAutoAllocationCapacityHours());
        r.setAutoIssue(s.getAutoIssue());
        r.setServiceAllocations(s.getServiceAllocations());
        r.setAllowGeneralUseExpressHours(s.getAllowGeneralUseExpressHours());
        return r;
    }

    private MechanicConfigurationSettingsRequest toSnapshot(MechanicConfigurationSettings c) {
        MechanicConfigurationSettingsRequest r = new MechanicConfigurationSettingsRequest();
        r.setJobCardNumberStartingSequence(c.getJobCardNumberStartingSequence());
        r.setJobCardNumberFormat(c.getJobCardNumberFormat());
        r.setJobCardNumberResetFrequency(c.getJobCardNumberResetFrequency());
        r.setJobCardNumberPrefix(c.getJobCardNumberPrefix());
        r.setJobCardNumberSuffix(c.getJobCardNumberSuffix());
        r.setRescheduleLimit(c.getRescheduleLimit());
        r.setRescheduleCutoffTime(c.getRescheduleCutoffTime());
        r.setPickupDropFacilityEnabled(c.getPickupDropFacilityEnabled());
        r.setAutoConfirmOtherState(c.getAutoConfirmOtherState());
        r.setRepairsRequireAdvance(c.getRepairsRequireAdvance());
        r.setServiceDueIntervalDays(c.getServiceDueIntervalDays());
        r.setSecondReminderIntervalDays(c.getSecondReminderIntervalDays());
        return r;
    }
}
