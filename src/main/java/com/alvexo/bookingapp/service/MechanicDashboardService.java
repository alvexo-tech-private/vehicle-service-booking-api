package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.MechanicDashboardResponse;
import com.alvexo.bookingapp.dto.response.ServiceBreakdownResponse;
import com.alvexo.bookingapp.dto.response.ServiceSlotSummaryResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSlotRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Backs the Home dashboard "Today" view. See WORKSHOP_SETTINGS_HOME_SPEC.md §3.
 */
@Service
public class MechanicDashboardService {

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository serviceSlotRepository;
    private final BookingRepository bookingRepository;

    public MechanicDashboardService(MechanicSettingsRepository settingsRepository,
                                     MechanicServiceSettingRepository serviceSettingRepository,
                                     MechanicServiceSlotRepository serviceSlotRepository,
                                     BookingRepository bookingRepository) {
        this.settingsRepository = settingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public MechanicDashboardResponse getTodayDashboard(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics have a dashboard");
        }

        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured yet"));

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        // Excludes CANCELLED/REJECTED — see BookingRepository#findMechanicBookingsBetween.
        List<Booking> todaysBookings = bookingRepository.findMechanicBookingsBetween(mechanic, dayStart, dayEnd);

        long totalBookedCount = todaysBookings.size();
        long autoConfirmedCount = todaysBookings.stream().filter(b -> b.getJobCardNumber() != null).count();
        long pendingCount = todaysBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();

        BigDecimal utilization = computeCapacityUtilization(settings, todaysBookings);

        List<ServiceBreakdownResponse> serviceBreakdown =
                buildServiceBreakdown(mechanic, todaysBookings);

        List<ServiceSlotSummaryResponse> slots = buildSlotSummary(mechanic, todaysBookings);

        return MechanicDashboardResponse.builder()
                .date(today)
                .jobCardType(settings.getJobCardType())
                .classification(settings.getClassification())
                .maxVehiclesPerDay(settings.getMaxVehiclesPerDay())
                .totalBookedCount(totalBookedCount)
                .autoConfirmedCount(autoConfirmedCount)
                .pendingCount(pendingCount)
                .capacityUtilizationPercent(utilization)
                .serviceBreakdown(serviceBreakdown)
                .slots(slots)
                .build();
    }

    private BigDecimal computeCapacityUtilization(MechanicSettings settings, List<Booking> todaysBookings) {
        if (Boolean.TRUE.equals(settings.getReserveCapacity())) {
            if (settings.getFullDayCapacityHours() == null
                    || settings.getFullDayCapacityHours().compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            int bookedMinutes = todaysBookings.stream()
                    .mapToInt(this::resolveDurationMinutes)
                    .sum();
            BigDecimal capacityMinutes = settings.getFullDayCapacityHours().multiply(BigDecimal.valueOf(60));
            return BigDecimal.valueOf(bookedMinutes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(capacityMinutes, 2, RoundingMode.HALF_UP);
        }

        if (settings.getMaxVehiclesPerDay() == null || settings.getMaxVehiclesPerDay() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(todaysBookings.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(settings.getMaxVehiclesPerDay()), 2, RoundingMode.HALF_UP);
    }

    private int resolveDurationMinutes(Booking b) {
        if (b.getServiceSetting() != null && b.getServiceSetting().getDurationMinutes() != null) {
            return b.getServiceSetting().getDurationMinutes();
        }
        return b.getEstimatedDurationMinutes() != null ? b.getEstimatedDurationMinutes() : 0;
    }

    private List<ServiceBreakdownResponse> buildServiceBreakdown(User mechanic, List<Booking> todaysBookings) {
        return serviceSettingRepository.findByMechanicAndIsActiveTrueOrderByDisplayOrderAsc(mechanic).stream()
                .map(service -> {
                    List<Booking> forService = todaysBookings.stream()
                            .filter(b -> b.getServiceSetting() != null
                                    && b.getServiceSetting().getId().equals(service.getId()))
                            .collect(Collectors.toList());

                    long issued = forService.stream().filter(b -> b.getJobCardNumber() != null).count();
                    long pending = forService.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();

                    return ServiceBreakdownResponse.builder()
                            .serviceId(service.getId())
                            .serviceName(service.getServiceName())
                            .category(service.getCategory())
                            .maxSlotsPerDay(service.getMaxSlotsPerDay())
                            .bookedCount((long) forService.size())
                            .issuedCount(issued)
                            .pendingCount(pending)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ServiceSlotSummaryResponse> buildSlotSummary(User mechanic, List<Booking> todaysBookings) {
        return serviceSlotRepository.findByMechanicOrderBySlotNumberAsc(mechanic).stream()
                .map(slot -> {
                    long issued = todaysBookings.stream()
                            .filter(b -> b.getJobCardNumber() != null)
                            .filter(b -> slot.getSlotTime().equals(b.getScheduledDateTime().toLocalTime()))
                            .count();

                    return ServiceSlotSummaryResponse.builder()
                            .slotNumber(slot.getSlotNumber())
                            .slotTime(slot.getSlotTime())
                            .enabled(slot.getEnabled())
                            .plannedCount(slot.getRepairQty())
                            .issuedCount(issued)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
