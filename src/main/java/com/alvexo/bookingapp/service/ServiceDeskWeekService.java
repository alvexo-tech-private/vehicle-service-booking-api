package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.ServiceCancelResponse;
import com.alvexo.bookingapp.dto.response.WeekDayResponse;
import com.alvexo.bookingapp.dto.response.WeekRescheduleResponse;
import com.alvexo.bookingapp.dto.response.WeekVehicleResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingChannel;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.WeekStatus;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicDailyOverrideRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.util.ServiceDeskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Backs the Service Desk "Service Week" tab (SERVICE_DESK_API_SPEC.md §2) — future dates only, today excluded. */
@Service
public class ServiceDeskWeekService {

    private static final int DEFAULT_WINDOW_DAYS = 7;

    private final BookingRepository bookingRepository;
    private final MechanicSettingsRepository mechanicSettingsRepository;
    private final MechanicDailyOverrideRepository dailyOverrideRepository;
    private final NotificationService notificationService;
    private final ServiceDeskTodayService todayService;

    public ServiceDeskWeekService(BookingRepository bookingRepository,
                                   MechanicSettingsRepository mechanicSettingsRepository,
                                   MechanicDailyOverrideRepository dailyOverrideRepository,
                                   NotificationService notificationService,
                                   ServiceDeskTodayService todayService) {
        this.bookingRepository = bookingRepository;
        this.mechanicSettingsRepository = mechanicSettingsRepository;
        this.dailyOverrideRepository = dailyOverrideRepository;
        this.notificationService = notificationService;
        this.todayService = todayService;
    }

    @Transactional(readOnly = true)
    public List<WeekDayResponse> getServiceWeek(User mechanic, LocalDate from, LocalDate to) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate effectiveFrom = (from == null || from.isBefore(tomorrow)) ? tomorrow : from;
        LocalDate effectiveTo = to != null ? to : effectiveFrom.plusDays(DEFAULT_WINDOW_DAYS - 1);
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("to must not be before from");
        }

        List<Booking> bookings = bookingRepository.findServiceWeekBookings(
                mechanic, effectiveFrom.atStartOfDay(), effectiveTo.atTime(LocalTime.MAX));
        Map<LocalDate, List<Booking>> byDate = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getScheduledDateTime().toLocalDate()));

        Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(mechanic);
        LocalDate today = LocalDate.now();

        return effectiveFrom.datesUntil(effectiveTo.plusDays(1))
                .map(date -> buildDayResponse(mechanic, date, today, byDate.getOrDefault(date, List.of()), settingsOpt))
                .collect(Collectors.toList());
    }

    @Transactional
    public WeekRescheduleResponse reschedule(User mechanic, String id, LocalDate toDate) {
        Booking booking = todayService.resolveBooking(mechanic, id);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule a " + booking.getStatus() + " booking");
        }
        LocalDate currentDate = booking.getScheduledDateTime().toLocalDate();
        if (toDate.equals(currentDate)) {
            throw new BadRequestException("toDate must be different from the booking's current date");
        }

        boolean overCapacity = isOverCapacity(mechanic, toDate);

        booking.setScheduledDateTime(LocalDateTime.of(toDate, booking.getScheduledDateTime().toLocalTime()));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setServiceStage(null);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getVehicleUser(),
                "Booking Rescheduled",
                "Your booking #" + booking.getBookingNumber() + " has been rescheduled to " + toDate,
                NotificationType.GENERAL,
                "Booking",
                booking.getId());

        return WeekRescheduleResponse.builder()
                .vehicle(toWeekVehicleResponse(booking))
                .overCapacity(overCapacity)
                .build();
    }

    @Transactional
    public ServiceCancelResponse cancel(User mechanic, String id, String message) {
        Booking booking = todayService.resolveBooking(mechanic, id);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("A " + booking.getStatus() + " booking cannot be cancelled");
        }

        var reliabilityAdjustment = todayService.applyCancellation(booking, mechanic, message);
        bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getVehicleUser(),
                "Booking Cancelled",
                "Booking #" + booking.getBookingNumber() + ": " + message,
                NotificationType.BOOKING_CANCELLED,
                "Booking",
                booking.getId());

        return ServiceCancelResponse.builder()
                .bookingId(booking.getBookingNumber())
                .cancellationMessage(booking.getCancellationMessage())
                .reliabilityAdjustment(reliabilityAdjustment)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private WeekDayResponse buildDayResponse(User mechanic, LocalDate date, LocalDate today,
                                              List<Booking> dayBookings, Optional<MechanicSettings> settingsOpt) {
        Integer capacity = resolveCapacity(mechanic, date, settingsOpt);

        long booked = dayBookings.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED).count();
        long riderBooked = dayBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getChannel() == BookingChannel.ONLINE || b.getChannel() == BookingChannel.RIDER_APP)
                .count();

        List<WeekVehicleResponse> vehicles = dayBookings.stream()
                .sorted(Comparator.comparing(Booking::getScheduledDateTime))
                .map(this::toWeekVehicleResponse)
                .collect(Collectors.toList());

        return WeekDayResponse.builder()
                .date(date)
                .offset((int) ChronoUnit.DAYS.between(today, date))
                .dow(date.getDayOfWeek())
                .capacity(capacity)
                .riderBooked(riderBooked)
                .booked(booked)
                .band(ServiceDeskMapper.capacityBand(booked, capacity))
                .vehicles(vehicles)
                .build();
    }

    private boolean isOverCapacity(User mechanic, LocalDate date) {
        Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(mechanic);
        Integer capacity = resolveCapacity(mechanic, date, settingsOpt);
        if (capacity == null) {
            return false;
        }
        long activeOnTarget = bookingRepository.countActiveByMechanicAndDate(mechanic, date);
        return (activeOnTarget + 1) > capacity;
    }

    private Integer resolveCapacity(User mechanic, LocalDate date, Optional<MechanicSettings> settingsOpt) {
        Optional<MechanicDailyOverride> overrideOpt = dailyOverrideRepository.findByMechanicAndDate(mechanic, date);
        Integer overridden = overrideOpt.map(MechanicDailyOverride::getMaxVehiclesPerDayOverride).orElse(null);
        if (overridden != null) {
            return overridden;
        }
        return settingsOpt.map(MechanicSettings::getMaxVehiclesPerDay).orElse(null);
    }

    private WeekVehicleResponse toWeekVehicleResponse(Booking booking) {
        WeekStatus status = ServiceDeskMapper.toWeekStatus(booking);
        String registrationNumber = todayService.resolveRegistrationNumber(booking);
        return WeekVehicleResponse.builder()
                .id(booking.getBookingNumber())
                .bookingId(booking.getBookingNumber())
                .ownerName(ServiceDeskMapper.fullName(
                        booking.getVehicleUser().getFirstName(), booking.getVehicleUser().getLastName()))
                .mobile(booking.getVehicleUser().getMobileNumber())
                .registrationNumber(registrationNumber)
                .make(booking.getVehicle().getMake())
                .model(booking.getVehicle().getModel())
                .regnLast4(ServiceDeskMapper.regnLast4(registrationNumber))
                .serviceType(ServiceDeskMapper.serviceTypeAbbr(booking))
                .pickupRequired(booking.getPickupRequired())
                .reportingTime(ServiceDeskMapper.formatReportingTime(booking.getScheduledDateTime()))
                .customerRemarks(booking.getCustomerNotes())
                .status(status)
                .cancellationMessage(status == WeekStatus.CANCELLED ? booking.getCancellationMessage() : null)
                .build();
    }
}
