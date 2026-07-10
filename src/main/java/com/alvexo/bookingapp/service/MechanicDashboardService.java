package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.BookingDetailResponse;
import com.alvexo.bookingapp.dto.response.MechanicDashboardResponse;
import com.alvexo.bookingapp.dto.response.ServiceBreakdownResponse;
import com.alvexo.bookingapp.dto.response.ServiceReminderResponse;
import com.alvexo.bookingapp.dto.response.ServiceSlotSummaryResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.bookingapp.repository.MechanicDailyOverrideRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSlotRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backs the Home dashboard "Today" view. See WORKSHOP_SETTINGS_HOME_SPEC.md §3.
 */
@Service
public class MechanicDashboardService {

    private static final int DEFAULT_SERVICE_DUE_INTERVAL_DAYS = 90;
    private static final int DEFAULT_SECOND_REMINDER_INTERVAL_DAYS = 15;

    private final MechanicSettingsRepository settingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository serviceSlotRepository;
    private final BookingRepository bookingRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final MechanicConfigurationSettingsRepository configurationSettingsRepository;
    private final MechanicDailyOverrideRepository dailyOverrideRepository;

    public MechanicDashboardService(MechanicSettingsRepository settingsRepository,
                                     MechanicServiceSettingRepository serviceSettingRepository,
                                     MechanicServiceSlotRepository serviceSlotRepository,
                                     BookingRepository bookingRepository,
                                     UserVehicleRepository userVehicleRepository,
                                     MechanicConfigurationSettingsRepository configurationSettingsRepository,
                                     MechanicDailyOverrideRepository dailyOverrideRepository) {
        this.settingsRepository = settingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.bookingRepository = bookingRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.configurationSettingsRepository = configurationSettingsRepository;
        this.dailyOverrideRepository = dailyOverrideRepository;
    }

    @Transactional(readOnly = true)
    public MechanicDashboardResponse getTodayDashboard(User mechanic) {
        return getDashboardForDate(mechanic, LocalDate.now());
    }

    /**
     * Same payload as getTodayDashboard, for an arbitrary date — backs the
     * 7-day strip (GET /api/mechanic-dashboard?date=).
     */
    @Transactional(readOnly = true)
    public MechanicDashboardResponse getDashboardForDate(User mechanic, LocalDate date) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics have a dashboard");
        }

        MechanicSettings settings = settingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured yet"));

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        // Excludes CANCELLED/REJECTED — see BookingRepository#findMechanicBookingsBetween.
        List<Booking> dayBookings = bookingRepository.findMechanicBookingsBetween(mechanic, dayStart, dayEnd);

        long totalBookedCount = dayBookings.size();
        long autoConfirmedCount = dayBookings.stream().filter(b -> b.getJobCardNumber() != null).count();
        long pendingCount = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long walkInCount = dayBookings.stream().filter(b -> b.getChannel() == BookingChannel.WALK_IN).count();
        long riderCount = dayBookings.stream().filter(b -> b.getChannel() == BookingChannel.RIDER_APP).count();

        BigDecimal utilization = computeCapacityUtilization(mechanic, settings, date, dayBookings);

        List<ServiceBreakdownResponse> serviceBreakdown =
                buildServiceBreakdown(mechanic, dayBookings);

        List<ServiceSlotSummaryResponse> slots = buildSlotSummary(mechanic, dayBookings);

        return MechanicDashboardResponse.builder()
                .date(date)
                .jobCardType(settings.getJobCardType())
                .classification(settings.getClassification())
                .maxVehiclesPerDay(settings.getMaxVehiclesPerDay())
                .totalBookedCount(totalBookedCount)
                .autoConfirmedCount(autoConfirmedCount)
                .pendingCount(pendingCount)
                .walkInCount(walkInCount)
                .riderCount(riderCount)
                .capacityUtilizationPercent(utilization)
                .serviceBreakdown(serviceBreakdown)
                .slots(slots)
                .build();
    }

    /** Waiting-list drill-down — vehicles behind pendingCount, for approve/reschedule actions. */
    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getPendingBookings(User mechanic, LocalDate date) {
        List<Booking> dayBookings = fetchDayBookings(mechanic, date);
        return dayBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    /** Issued-JC drill-down for one service — vehicles behind a service row's issuedCount. */
    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getIssuedBookingsForService(User mechanic, Long serviceId, LocalDate date) {
        MechanicServiceSetting service = serviceSettingRepository.findByIdAndMechanic(serviceId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));

        List<Booking> dayBookings = fetchDayBookings(mechanic, date);
        return dayBookings.stream()
                .filter(b -> b.getJobCardNumber() != null)
                .filter(b -> b.getServiceSetting() != null && b.getServiceSetting().getId().equals(service.getId()))
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    private List<Booking> fetchDayBookings(User mechanic, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return bookingRepository.findMechanicBookingsBetween(mechanic, dayStart, dayEnd);
    }

    private BookingDetailResponse toDetailResponse(Booking b) {
        String registrationNumber = userVehicleRepository.findByUserAndVehicle(b.getVehicleUser(), b.getVehicle())
                .map(uv -> uv.getRegistrationNumber())
                .orElse(null);

        return BookingDetailResponse.builder()
                .bookingId(b.getId())
                .bookingNumber(b.getBookingNumber())
                .jobCardNumber(b.getJobCardNumber())
                .vehicleRegistrationNumber(registrationNumber)
                .vehicleInfo(b.getVehicle().getMake() + " " + b.getVehicle().getModel())
                .customerName(b.getVehicleUser().getFirstName() + " " + b.getVehicleUser().getLastName())
                .customerPhone(b.getVehicleUser().getMobileNumber())
                .serviceSettingName(b.getServiceSetting() != null ? b.getServiceSetting().getServiceName() : null)
                .scheduledDateTime(b.getScheduledDateTime())
                .status(b.getStatus())
                .channel(b.getChannel())
                .build();
    }

    /**
     * Service Due / Second Reminder card (spec §7 #74/#75). For each vehicle
     * this mechanic has completed a booking for, flags it DUE once
     * daysSinceService >= serviceDueIntervalDays, and SECOND_REMINDER once it
     * also passes + secondReminderIntervalDays. Uses the mechanic's
     * configuration settings if set up, else the documented defaults (90/15).
     */
    @Transactional(readOnly = true)
    public List<ServiceReminderResponse> getServiceReminders(User mechanic) {
        int serviceDueIntervalDays = DEFAULT_SERVICE_DUE_INTERVAL_DAYS;
        int secondReminderIntervalDays = DEFAULT_SECOND_REMINDER_INTERVAL_DAYS;

        var configOpt = configurationSettingsRepository.findByMechanic(mechanic);
        if (configOpt.isPresent()) {
            serviceDueIntervalDays = configOpt.get().getServiceDueIntervalDays();
            secondReminderIntervalDays = configOpt.get().getSecondReminderIntervalDays();
        }

        List<Booking> completed = bookingRepository.findCompletedBookingsByMechanicOrderByCompletedAtDesc(mechanic);

        // Keep only the most recent completed booking per vehicle (list is already ordered desc).
        Map<Long, Booking> latestPerVehicle = new LinkedHashMap<>();
        for (Booking b : completed) {
            latestPerVehicle.putIfAbsent(b.getVehicle().getId(), b);
        }

        LocalDate today = LocalDate.now();
        final int dueThreshold = serviceDueIntervalDays;
        final int secondReminderThreshold = serviceDueIntervalDays + secondReminderIntervalDays;

        return latestPerVehicle.values().stream()
                .map(b -> {
                    LocalDate lastServiceDate = b.getCompletedAt().toLocalDate();
                    int daysSince = (int) ChronoUnit.DAYS.between(lastServiceDate, today);
                    ReminderStage stage = daysSince >= secondReminderThreshold ? ReminderStage.SECOND_REMINDER
                            : daysSince >= dueThreshold ? ReminderStage.DUE : null;
                    if (stage == null) {
                        return null;
                    }

                    String registrationNumber = userVehicleRepository
                            .findByUserAndVehicle(b.getVehicleUser(), b.getVehicle())
                            .map(uv -> uv.getRegistrationNumber())
                            .orElse(null);

                    return ServiceReminderResponse.builder()
                            .vehicleId(b.getVehicle().getId())
                            .vehicleRegistrationNumber(registrationNumber)
                            .vehicleInfo(b.getVehicle().getMake() + " " + b.getVehicle().getModel())
                            .customerId(b.getVehicleUser().getId())
                            .customerName(b.getVehicleUser().getFirstName() + " " + b.getVehicleUser().getLastName())
                            .customerPhone(b.getVehicleUser().getMobileNumber())
                            .lastServiceDate(lastServiceDate)
                            .daysSinceService(daysSince)
                            .reminderStage(stage)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BigDecimal computeCapacityUtilization(User mechanic, MechanicSettings settings, LocalDate date,
                                                   List<Booking> dayBookings) {
        var overrideOpt = dailyOverrideRepository.findByMechanicAndDate(mechanic, date);

        if (Boolean.TRUE.equals(settings.getReserveCapacity())) {
            BigDecimal fullDayCapacityHours = overrideOpt.map(MechanicDailyOverride::getFullDayCapacityHoursOverride)
                    .orElse(settings.getFullDayCapacityHours());
            if (fullDayCapacityHours == null || fullDayCapacityHours.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            int bookedMinutes = dayBookings.stream()
                    .mapToInt(this::resolveDurationMinutes)
                    .sum();
            BigDecimal capacityMinutes = fullDayCapacityHours.multiply(BigDecimal.valueOf(60));
            return BigDecimal.valueOf(bookedMinutes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(capacityMinutes, 2, RoundingMode.HALF_UP);
        }

        Integer maxVehiclesPerDay = overrideOpt.map(MechanicDailyOverride::getMaxVehiclesPerDayOverride)
                .orElse(settings.getMaxVehiclesPerDay());
        if (maxVehiclesPerDay == null || maxVehiclesPerDay <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(dayBookings.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxVehiclesPerDay), 2, RoundingMode.HALF_UP);
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
