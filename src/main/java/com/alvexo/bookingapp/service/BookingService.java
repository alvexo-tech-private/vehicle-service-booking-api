package com.alvexo.bookingapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.BookingRequest;
import com.alvexo.bookingapp.dto.request.WalkInBookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingChannel;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.BookingType;
import com.alvexo.bookingapp.model.DayOfWeek;
import com.alvexo.bookingapp.model.MechanicAvailability;
import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.MechanicServiceSetting;
import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicAvailabilityRepository availabilityRepository;
    private final MechanicSettingsRepository mechanicSettingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final NotificationService notificationService;
    private final MechanicHolidayService holidayService;
    private final MechanicDailyOverrideService dailyOverrideService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            MechanicAvailabilityRepository availabilityRepository,
            MechanicSettingsRepository mechanicSettingsRepository,
            MechanicServiceSettingRepository serviceSettingRepository,
            NotificationService notificationService,
            MechanicHolidayService holidayService,
            MechanicDailyOverrideService dailyOverrideService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityRepository = availabilityRepository;
        this.mechanicSettingsRepository = mechanicSettingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.notificationService = notificationService;
        this.holidayService = holidayService;
        this.dailyOverrideService = dailyOverrideService;
    }

    /**
     * Creates a booking with full concurrency protection.
     *
     * Capacity strategy — driven by mechanic's reserveCapacity setting:
     *
     * ── Vehicle-count mode (reserveCapacity = false) ──────────────────────────
     *   Counts active (non-cancelled) bookings for the day.
     *   Rejects if count >= maxVehiclesPerDay.
     *
     * ── Hour-slot mode (reserveCapacity = true) ───────────────────────────────
     *   Sums duration_minutes of all active bookings for the day.
     *   Rejects if (sum + newServiceDuration) > fullDayCapacityHours * 60.
     *   Also checks per-service cap (maxSlotsPerDay) when set.
     *   Determines bookingType (STANDARD / EXPRESS) from scheduled time
     *   vs mechanic's expressReportingTime.
     *
     * Advance payment:
     *   If mechanic.advanceEnabled = true, request.advancePaid must equal
     *   mechanic.advanceAmount for the booking to be accepted.
     *
     * Concurrency layers:
     *   Layer 1 (application): PESSIMISTIC_WRITE lock on conflicting bookings.
     *   Layer 2 (database):    unique constraint on (mechanic_id, scheduled_date_time).
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User vehicleUser) {

        // 1. Validate mechanic
        User mechanic = userRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Selected user is not a mechanic");
        }

        // 2. Validate vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        // 3. Reject past-dated bookings
        if (request.getScheduledDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Bookings cannot be made for past dates");
        }

        // 3b. Reject bookings on a Holiday/Pause date
        rejectIfHolidayOrPause(mechanic, request.getScheduledDateTime().toLocalDate());

        // 4. Validate slot within mechanic's availability template
        validateSlotWithinAvailability(mechanic, request.getScheduledDateTime());

        // 5. Load mechanic settings (optional — some mechanics may not have configured yet)
        Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(mechanic);
        LocalDate bookingDate = request.getScheduledDateTime().toLocalDate();

        // 6. Resolve service setting (if provided)
        MechanicServiceSetting serviceSetting = null;
        if (request.getServiceSettingId() != null) {
            serviceSetting = serviceSettingRepository.findById(request.getServiceSettingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));
            if (!serviceSetting.getMechanic().getId().equals(mechanic.getId())) {
                throw new BadRequestException("Service does not belong to the selected mechanic");
            }
            if (!Boolean.TRUE.equals(serviceSetting.getIsActive())) {
                throw new BadRequestException("Selected service is no longer available");
            }
        }

        // 7. Settings-aware capacity checks
        BookingType bookingType = checkCapacityAndResolveBookingType(
                mechanic, serviceSetting, bookingDate, request.getScheduledDateTime(),
                request.getAdvancePaid(), settingsOpt);

        // 8. PESSIMISTIC LOCK — concurrency layer 1
        List<Booking> conflicts = bookingRepository.findAndLockConflicting(
                mechanic, request.getScheduledDateTime());
        if (!conflicts.isEmpty()) {
            throw new BadRequestException(
                    "This slot is already booked. Please choose a different time.");
        }

        // 9. Build and save — DB unique constraint is the final guard
        Booking booking = Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .serviceSetting(serviceSetting)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(request.getScheduledDateTime())
                .status(BookingStatus.PENDING)
                .bookingType(bookingType)
                .channel(BookingChannel.ONLINE)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDurationMinutes(
                        serviceSetting != null
                        ? serviceSetting.getDurationMinutes()
                        : request.getEstimatedDurationMinutes())
                .advancePaid(request.getAdvancePaid() != null
                        ? request.getAdvancePaid() : BigDecimal.ZERO)
                .customerNotes(request.getCustomerNotes())
                .build();

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_mechanic_slot")) {
                throw new BadRequestException(
                        "This slot was just taken by another booking. Please choose a different time.");
            }
            throw e;
        }

        // 10. Notify mechanic
        notificationService.createNotification(
                mechanic,
                "New Booking Request",
                "You have a new " + bookingType.name().toLowerCase() +
                " booking request from " + vehicleUser.getFirstName(),
                NotificationType.BOOKING_CREATED,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Mechanic-recorded walk-in booking. Confirmed immediately (no PENDING
     * approval step, since the customer is physically present) and tagged
     * channel = WALK_IN for Home dashboard counters. Still subject to the
     * same capacity/advance rules as an online booking.
     */
    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request, User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can record walk-in bookings");
        }

        User vehicleUser = userRepository.findById(request.getVehicleUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle user not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        LocalDateTime scheduledDateTime = request.getScheduledDateTime() != null
                ? request.getScheduledDateTime() : LocalDateTime.now();
        LocalDate bookingDate = scheduledDateTime.toLocalDate();

        rejectIfHolidayOrPause(mechanic, bookingDate);

        MechanicServiceSetting serviceSetting = null;
        if (request.getServiceSettingId() != null) {
            serviceSetting = serviceSettingRepository.findById(request.getServiceSettingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));
            if (!serviceSetting.getMechanic().getId().equals(mechanic.getId())) {
                throw new BadRequestException("Service does not belong to this mechanic");
            }
        }

        Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(mechanic);
        BookingType bookingType = checkCapacityAndResolveBookingType(
                mechanic, serviceSetting, bookingDate, scheduledDateTime,
                request.getAdvancePaid(), settingsOpt);

        List<Booking> conflicts = bookingRepository.findAndLockConflicting(mechanic, scheduledDateTime);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("This slot is already booked. Please choose a different time.");
        }

        boolean autoIssue = settingsOpt.map(MechanicSettings::getAutoIssue).orElse(true);

        Booking booking = Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .serviceSetting(serviceSetting)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(scheduledDateTime)
                .status(BookingStatus.CONFIRMED)
                .bookingType(bookingType)
                .channel(BookingChannel.WALK_IN)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDurationMinutes(
                        serviceSetting != null
                        ? serviceSetting.getDurationMinutes()
                        : request.getEstimatedDurationMinutes())
                .advancePaid(request.getAdvancePaid() != null
                        ? request.getAdvancePaid() : BigDecimal.ZERO)
                .customerNotes(request.getCustomerNotes())
                .build();

        if (autoIssue) {
            booking.setJobCardNumber(generateJobCardNumber(booking));
        }

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_mechanic_slot")) {
                throw new BadRequestException(
                        "This slot was just taken by another booking. Please choose a different time.");
            }
            throw e;
        }

        return convertToResponse(booking);
    }

    /**
     * Shared capacity/advance validation for both online and walk-in bookings.
     * Returns the resolved BookingType (STANDARD/EXPRESS); throws BadRequestException
     * if capacity is exhausted or the required advance hasn't been paid.
     */
    private BookingType checkCapacityAndResolveBookingType(User mechanic,
                                                            MechanicServiceSetting serviceSetting,
                                                            LocalDate bookingDate,
                                                            LocalDateTime scheduledDateTime,
                                                            BigDecimal advancePaid,
                                                            Optional<MechanicSettings> settingsOpt) {
        BookingType bookingType = BookingType.STANDARD;
        if (settingsOpt.isEmpty()) {
            return bookingType;
        }

        MechanicSettings settings = settingsOpt.get();
        Optional<MechanicDailyOverride> overrideOpt = dailyOverrideService.findOverride(mechanic, bookingDate);

        BigDecimal fullDayCapacityHours = overrideOpt.map(MechanicDailyOverride::getFullDayCapacityHoursOverride)
                .orElse(settings.getFullDayCapacityHours());
        Integer maxVehiclesPerDay = overrideOpt.map(MechanicDailyOverride::getMaxVehiclesPerDayOverride)
                .orElse(settings.getMaxVehiclesPerDay());
        Boolean advanceEnabled = overrideOpt.map(MechanicDailyOverride::getAdvanceEnabledOverride)
                .orElse(settings.getAdvanceEnabled());
        BigDecimal advanceAmount = overrideOpt.map(MechanicDailyOverride::getAdvanceAmountOverride)
                .orElse(settings.getAdvanceAmount());

        if (Boolean.TRUE.equals(settings.getReserveCapacity())) {
            // ── Hour-slot mode ────────────────────────────────────────────
            if (serviceSetting == null) {
                throw new BadRequestException(
                        "serviceSettingId is required for this mechanic (hour-slot mode)");
            }

            int durationMinutes = serviceSetting.getDurationMinutes();

            int bookedMinutes = serviceSettingRepository
                    .sumBookedMinutesForDate(mechanic.getId(), bookingDate);
            int capacityMinutes = fullDayCapacityHours
                    .multiply(BigDecimal.valueOf(60)).intValue();

            if ((bookedMinutes + durationMinutes) > capacityMinutes) {
                throw new BadRequestException(
                        "Mechanic's daily capacity is full for " + bookingDate +
                        ". Available: " + (capacityMinutes - bookedMinutes) + " min.");
            }

            if (serviceSetting.getMaxSlotsPerDay() != null) {
                int serviceCount = serviceSettingRepository
                        .countBookingsForServiceOnDate(serviceSetting.getId(), bookingDate);
                if (serviceCount >= serviceSetting.getMaxSlotsPerDay()) {
                    throw new BadRequestException(
                            "No more slots available for service '" +
                            serviceSetting.getServiceName() + "' on " + bookingDate);
                }
            }

            if (settings.getExpressReportingTime() != null &&
                scheduledDateTime.toLocalTime().isBefore(settings.getExpressReportingTime())) {

                if (!Boolean.TRUE.equals(serviceSetting.getIsExpressEligible())) {
                    throw new BadRequestException(
                            "Service '" + serviceSetting.getServiceName() +
                            "' is not eligible for express booking");
                }
                bookingType = BookingType.EXPRESS;
            }

        } else {
            // ── Vehicle-count mode ────────────────────────────────────────
            long activeCount = bookingRepository.countActiveBookingsForMechanicOnDate(mechanic, bookingDate);
            if (activeCount >= maxVehiclesPerDay) {
                throw new BadRequestException(
                        "Mechanic has reached the maximum vehicle limit (" +
                        maxVehiclesPerDay + ") for " + bookingDate);
            }
        }

        if (Boolean.TRUE.equals(advanceEnabled) && advanceAmount != null) {
            BigDecimal paidAdvance = advancePaid != null ? advancePaid : BigDecimal.ZERO;
            if (paidAdvance.compareTo(advanceAmount) < 0) {
                throw new BadRequestException(
                        "Advance payment of INR " + advanceAmount + " is required to confirm this booking");
            }
        }

        return bookingType;
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus newStatus, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.REJECTED) {
            if (!booking.getMechanic().getId().equals(user.getId())) {
                throw new BadRequestException("Only the assigned mechanic can confirm/reject bookings");
            }
        }

        // On confirmation, generate job card number if not already set —
        // unless the mechanic has autoIssue = false, in which case a job card
        // must be issued explicitly via issueJobCard().
        if (newStatus == BookingStatus.CONFIRMED && booking.getJobCardNumber() == null) {
            boolean autoIssue = mechanicSettingsRepository.findByMechanic(booking.getMechanic())
                    .map(MechanicSettings::getAutoIssue)
                    .orElse(true);
            if (autoIssue) {
                booking.setJobCardNumber(generateJobCardNumber(booking));
            }
        }

        booking.setStatus(newStatus);

        if (newStatus == BookingStatus.COMPLETED) {
            booking.setCompletedAt(LocalDateTime.now());
            User mechanic = booking.getMechanic();
            mechanic.setTotalBookingsCompleted(mechanic.getTotalBookingsCompleted() + 1);
            userRepository.save(mechanic);
        } else if (newStatus == BookingStatus.CANCELLED) {
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancelledBy(user);
        }

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("This slot is no longer available. Please choose another.");
        }

        User recipient = booking.getMechanic().getId().equals(user.getId())
                ? booking.getVehicleUser() : booking.getMechanic();

        notificationService.createNotification(
                recipient,
                "Booking Status Updated",
                "Booking #" + booking.getBookingNumber() + " status: " + newStatus,
                NotificationType.valueOf("BOOKING_" + newStatus),
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Reschedules a booking to a new date/time. Re-runs the same conflict
     * checks as booking creation; does not re-run capacity checks, since the
     * booking already holds a capacity slot for that day (rescheduling within
     * the same mechanic's calendar doesn't change total daily demand).
     */
    @Transactional
    public BookingResponse rescheduleBooking(Long bookingId, LocalDateTime newScheduledDateTime, User mechanic) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("Only the assigned mechanic can reschedule this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule a " + booking.getStatus() + " booking");
        }
        if (newScheduledDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot reschedule to a past date/time");
        }

        List<Booking> conflicts = bookingRepository.findAndLockConflicting(mechanic, newScheduledDateTime);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("The requested time is already booked. Please choose a different time.");
        }

        booking.setScheduledDateTime(newScheduledDateTime);

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("This slot was just taken. Please choose another.");
        }

        notificationService.createNotification(
                booking.getVehicleUser(),
                "Booking Rescheduled",
                "Your booking #" + booking.getBookingNumber() + " has been rescheduled to " + newScheduledDateTime,
                NotificationType.GENERAL,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Manually issues a job card for a CONFIRMED booking whose mechanic has
     * autoIssue = false. No-op error if a job card already exists.
     */
    @Transactional
    public BookingResponse issueJobCard(Long bookingId, User mechanic) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("Only the assigned mechanic can issue this job card");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Job cards can only be issued for CONFIRMED bookings");
        }
        if (booking.getJobCardNumber() != null) {
            throw new BadRequestException("Job card already issued: " + booking.getJobCardNumber());
        }

        booking.setJobCardNumber(generateJobCardNumber(booking));
        booking = bookingRepository.save(booking);
        return convertToResponse(booking);
    }

    public Page<BookingResponse> getUserBookings(User user, Pageable pageable) {
        if (user.getRole() == UserRole.MECHANIC) {
            return bookingRepository.findByMechanic(user, pageable).map(this::convertToResponse);
        }
        return bookingRepository.findByVehicleUser(user, pageable).map(this::convertToResponse);
    }

    public BookingResponse getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getVehicleUser().getId().equals(user.getId())
                && !booking.getMechanic().getId().equals(user.getId())
                && user.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("You don't have access to this booking");
        }

        return convertToResponse(booking);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void rejectIfHolidayOrPause(User mechanic, LocalDate date) {
        holidayService.findHoliday(mechanic, date).ifPresent(holiday -> {
            throw new BadRequestException(
                    "Mechanic is on " + holiday.getType().name().toLowerCase() + " on " + date
                            + (holiday.getReason() != null ? " (" + holiday.getReason() + ")" : ""));
        });
    }

    private void validateSlotWithinAvailability(User mechanic, LocalDateTime scheduledDateTime) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(scheduledDateTime.getDayOfWeek().name());
        List<MechanicAvailability> rules = availabilityRepository
                .findByMechanicAndDayOfWeek(mechanic, dayOfWeek);

        if (rules.isEmpty()) {
            throw new BadRequestException("Mechanic is not available on " + dayOfWeek);
        }

        boolean withinAnyRule = rules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsAvailable()))
                .anyMatch(r -> {
                    var slotTime = scheduledDateTime.toLocalTime();
                    return !slotTime.isBefore(r.getStartTime())
                            && !slotTime.isAfter(r.getEndTime().minusMinutes(
                                    r.getSlotDurationMinutes() != null ? r.getSlotDurationMinutes() : 60));
                });

        if (!withinAnyRule) {
            throw new BadRequestException(
                    "The requested time is outside the mechanic's working hours for that day.");
        }
    }

    /**
     * Generates job card number on booking confirmation.
     * Format: {prefix}{YYMMDD}{2-digit daily sequence}
     * e.g. prefix="0101", date=2025-04-08, seq=1 → "010125040801"
     */
    private String generateJobCardNumber(Booking booking) {
        Optional<MechanicSettings> settingsOpt =
                mechanicSettingsRepository.findByMechanic(booking.getMechanic());

        String prefix = settingsOpt.map(MechanicSettings::getJobCardSerialPrefix).orElse("JC");
        LocalDate date = booking.getScheduledDateTime().toLocalDate();
        String datePart = String.format("%02d%02d%02d",
                date.getYear() % 100, date.getMonthValue(), date.getDayOfMonth());

        long dailySeq = bookingRepository.countByMechanicAndJobCardNumberStartingWith(
                booking.getMechanic(), prefix + datePart) + 1;

        return prefix + datePart + String.format("%02d", dailySeq);
    }

    private BookingResponse convertToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .jobCardNumber(booking.getJobCardNumber())
                .vehicleUserId(booking.getVehicleUser().getId())
                .vehicleUserName(booking.getVehicleUser().getFirstName() + " " +
                                 booking.getVehicleUser().getLastName())
                .mechanicId(booking.getMechanic().getId())
                .mechanicName(booking.getMechanic().getFirstName() + " " +
                              booking.getMechanic().getLastName())
                .vehicleId(booking.getVehicle().getId())
                .vehicleInfo(booking.getVehicle().getMake() + " " +
                             booking.getVehicle().getModel() + " " +
                             booking.getVehicle().getYear())
                .serviceSettingId(booking.getServiceSetting() != null
                        ? booking.getServiceSetting().getId() : null)
                .serviceSettingName(booking.getServiceSetting() != null
                        ? booking.getServiceSetting().getServiceName() : null)
                .scheduledDateTime(booking.getScheduledDateTime())
                .status(booking.getStatus())
                .bookingType(booking.getBookingType())
                .channel(booking.getChannel())
                .serviceType(booking.getServiceType())
                .description(booking.getDescription())
                .estimatedCost(booking.getEstimatedCost())
                .actualCost(booking.getActualCost())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .actualDurationMinutes(booking.getActualDurationMinutes())
                .advancePaid(booking.getAdvancePaid())
                .mechanicNotes(booking.getMechanicNotes())
                .customerNotes(booking.getCustomerNotes())
                .cancellationReason(booking.getCancellationReason())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private String generateBookingNumber() {
        return "BKG" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
