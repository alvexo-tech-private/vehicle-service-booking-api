package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.AvailabilityCheckResponse;
import com.alvexo.bookingapp.dto.response.DashboardMetricsResponse;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MechanicDashboardService {

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicSettingsService settingsService;
    private final BookingRepository bookingRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository slotRepository;
    private final MechanicTechnicianCapacityRepository technicianRepository;
    private final DailyQuotaOverrideRepository overrideRepository;

    public MechanicDashboardService(MechanicSettingsRepository settingsRepository,
                                     MechanicSettingsService settingsService,
                                     BookingRepository bookingRepository,
                                     MechanicServiceSettingRepository serviceSettingRepository,
                                     MechanicServiceSlotRepository slotRepository,
                                     MechanicTechnicianCapacityRepository technicianRepository,
                                     DailyQuotaOverrideRepository overrideRepository) {
        this.settingsRepository = settingsRepository;
        this.settingsService = settingsService;
        this.bookingRepository = bookingRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.slotRepository = slotRepository;
        this.technicianRepository = technicianRepository;
        this.overrideRepository = overrideRepository;
    }

    // ── Availability check (for rider UI grey-out) ───────────────────────────

    @Transactional(readOnly = true)
    public AvailabilityCheckResponse getAvailability(Long mechanicId, LocalDate date) {
        MechanicSettings settings = settingsRepository.findByMechanicId(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found for mechanic " + mechanicId));

        BigDecimal effectiveCapacity = settingsService.getEffectiveCapacityHours(settings, date);
        long totalMinutes = bookingRepository.sumAllBookedMinutesForDate(mechanicId, date);
        BigDecimal usedCapacity = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        boolean isFullyBooked = effectiveCapacity != null
                && usedCapacity.compareTo(effectiveCapacity) >= 0;

        // Per-category availability
        Map<ServiceCategory, Integer> availableByCategory = new EnumMap<>(ServiceCategory.class);
        List<MechanicServiceSetting> services = serviceSettingRepository
                .findByMechanicAndIsActiveTrueOrderByDisplayOrderAsc(settings.getMechanic());

        for (MechanicServiceSetting svc : services) {
            ServiceCategory cat = svc.getCategory();
            if (svc.getMaxSlotsPerDay() != null) {
                long booked = bookingRepository.countBookingsForServiceOnDate(svc.getId(), date);
                int available = Math.max(0, svc.getMaxSlotsPerDay() - (int) booked);
                availableByCategory.merge(cat, available, Integer::sum);
            } else {
                availableByCategory.putIfAbsent(cat, isFullyBooked ? 0 : 99);
            }
        }

        // Slot availability (TYPE_4)
        List<AvailabilityCheckResponse.SlotAvailability> slotAvailabilities = new ArrayList<>();
        if (settings.getJobCardType() == JobCardType.TYPE_4) {
            List<MechanicServiceSlot> slots = slotRepository
                    .findByMechanicSettingsAndIsEnabledTrue(settings);

            for (MechanicServiceSlot slot : slots) {
                long bookedCount = bookingRepository.countSlotBookingsForDate(slot.getId(), date);
                boolean slotApplicable = slot.isApplicableOn(date.getDayOfWeek());

                slotAvailabilities.add(AvailabilityCheckResponse.SlotAvailability.builder()
                        .slotId(slot.getId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .restrictedCategory(slot.getRestrictedCategory())
                        .maxVehicleQty(slot.getMaxVehicleQty())
                        .bookedCount(bookedCount)
                        .isAvailable(slotApplicable && bookedCount < slot.getMaxVehicleQty())
                        .applicableDays(slot.getApplicableDays())
                        .build());
            }
        }

        return AvailabilityCheckResponse.builder()
                .mechanicId(mechanicId)
                .date(date)
                .jobCardType(settings.getJobCardType())
                .effectiveCapacity(effectiveCapacity)
                .usedCapacity(usedCapacity)
                .isFullyBooked(isFullyBooked)
                .availableByCategory(availableByCategory)
                .slots(slotAvailabilities)
                .build();
    }

    // ── Today's dashboard metrics (for mechanic home screen) ─────────────────

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getTodayMetrics(User mechanic) {
        LocalDate today = LocalDate.now();

        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured"));

        Long mechanicId = mechanic.getId();
        BigDecimal effectiveCapacity = settingsService.getEffectiveCapacityHours(settings, today);

        long totalMinutes = bookingRepository.sumAllBookedMinutesForDate(mechanicId, today);
        BigDecimal totalBookedHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal remainingHours = effectiveCapacity != null
                ? effectiveCapacity.subtract(totalBookedHours).max(BigDecimal.ZERO)
                : null;

        long totalBookedCount = bookingRepository.countActiveBookingsForMechanicOnDate(mechanic, today);

        // Counts by allocation result
        long autoConfirmedCount = bookingRepository.sumBookedMinutesByAllocationResult(
                mechanicId, today, AllocationResult.AUTO_CONFIRMED) > 0
                ? countByAllocationResult(mechanicId, today, AllocationResult.AUTO_CONFIRMED)
                : 0;

        // Counts by booking source
        long riderAppCount = 0;
        long walkInCount = 0;
        long pendingReviewCount = 0;

        List<Object[]> sourceCounts = bookingRepository.countBookingsBySourceForDate(mechanicId, today);
        for (Object[] row : sourceCounts) {
            BookingSource source = (BookingSource) row[0];
            long count = (Long) row[1];
            if (source == BookingSource.RIDER_APP) riderAppCount = count;
            else if (source == BookingSource.WALK_IN) walkInCount = count;
        }

        pendingReviewCount = totalBookedCount - autoConfirmedCount - walkInCount;
        if (pendingReviewCount < 0) pendingReviewCount = 0;

        // Override check
        boolean overrideActive = overrideRepository
                .findByMechanicSettingsAndOverrideDateAndIsActiveTrue(settings, today)
                .isPresent();

        // Technician summary
        var allTechnicians = technicianRepository.findByMechanicSettings(settings);
        var activeTechnicians = allTechnicians.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());
        BigDecimal totalReservedHours = technicianRepository.sumActiveReservedHours(settings);

        DashboardMetricsResponse.TechnicianSummary techSummary =
                DashboardMetricsResponse.TechnicianSummary.builder()
                        .totalTechnicians((long) allTechnicians.size())
                        .activeTechnicians((long) activeTechnicians.size())
                        .totalReservedHours(totalReservedHours)
                        .build();

        // Slot utilization (TYPE_4)
        List<DashboardMetricsResponse.SlotUtilization> slotUtils = new ArrayList<>();
        if (settings.getJobCardType() == JobCardType.TYPE_4) {
            List<MechanicServiceSlot> slots = slotRepository
                    .findByMechanicSettingsAndIsEnabledTrue(settings);

            for (MechanicServiceSlot slot : slots) {
                long slotBooked = bookingRepository.countSlotBookingsForDate(slot.getId(), today);
                long slotAutoConfirmed = bookingRepository.countAutoConfirmedSlotBookingsForDate(
                        slot.getId(), today);

                slotUtils.add(DashboardMetricsResponse.SlotUtilization.builder()
                        .slotId(slot.getId())
                        .restrictedCategory(slot.getRestrictedCategory())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .maxVehicleQty(slot.getMaxVehicleQty())
                        .bookedCount(slotBooked)
                        .autoConfirmedCount(slotAutoConfirmed)
                        .pendingReviewCount(slotBooked - slotAutoConfirmed)
                        .applicableDays(slot.getApplicableDays())
                        .build());
            }
        }

        return DashboardMetricsResponse.builder()
                .date(today)
                .jobCardType(settings.getJobCardType())
                .effectiveCapacityHours(effectiveCapacity)
                .totalBookedCount(totalBookedCount)
                .totalBookedHours(totalBookedHours)
                .remainingCapacityHours(remainingHours)
                .autoConfirmedCount(autoConfirmedCount)
                .pendingReviewCount(pendingReviewCount)
                .riderAppCount(riderAppCount)
                .walkInCount(walkInCount)
                .quotaOverrideActive(overrideActive)
                .technicianSummary(techSummary)
                .slotUtilization(slotUtils)
                .build();
    }

    private long countByAllocationResult(Long mechanicId, LocalDate date, AllocationResult result) {
        return bookingRepository.sumBookedMinutesByAllocationResult(mechanicId, date, result) >= 0
                ? bookingRepository.countActiveBookingsForMechanicOnDate(
                        settingsRepository.findByMechanicId(mechanicId).get().getMechanic(), date)
                : 0;
    }
}
