package com.alvexo.bookingapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.BookingRequest;
import com.alvexo.bookingapp.dto.request.WalkInBookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingChannel;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.BookingType;
import com.alvexo.bookingapp.model.DayOfWeek;
import com.alvexo.bookingapp.model.JobCardType;
import com.alvexo.bookingapp.model.MechanicAvailability;
import com.alvexo.bookingapp.model.MechanicDailyOverride;
import com.alvexo.bookingapp.model.MechanicServiceSetting;
import com.alvexo.bookingapp.model.MechanicServiceSlot;
import com.alvexo.bookingapp.model.MechanicSettings;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.Payment;
import com.alvexo.bookingapp.model.PaymentStatus;
import com.alvexo.bookingapp.model.PaymentType;
import com.alvexo.bookingapp.model.ServiceType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.model.WorkshopBookingSequence;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSettingRepository;
import com.alvexo.bookingapp.repository.MechanicServiceSlotRepository;
import com.alvexo.bookingapp.repository.MechanicSettingsRepository;
import com.alvexo.bookingapp.repository.PaymentRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;
import com.alvexo.bookingapp.util.Constants;

@Service
public class BookingService {

    // Default working-hours fallback — applied when a workshop has no explicit
    // mechanic_availability rows. Matches AuthService.seedDefaultAvailability().
    private static final LocalTime DEFAULT_OPEN_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_OPEN_END   = LocalTime.of(18, 0);
    private static final java.util.EnumSet<DayOfWeek> DEFAULT_CLOSED_DAYS =
            java.util.EnumSet.of(DayOfWeek.SUNDAY);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicAvailabilityRepository availabilityRepository;
    private final MechanicSettingsRepository mechanicSettingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository serviceSlotRepository;
    private final NotificationService notificationService;
    private final MechanicHolidayService holidayService;
    private final MechanicDailyOverrideService dailyOverrideService;
    private final UserVehicleRepository userVehicleRepository;
    private final ReminderCycleService reminderCycleService;
    private final PaymentRepository paymentRepository;
    private final com.alvexo.bookingapp.repository.WorkshopBookingSequenceRepository workshopBookingSequenceRepository;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            MechanicAvailabilityRepository availabilityRepository,
            MechanicSettingsRepository mechanicSettingsRepository,
            MechanicServiceSettingRepository serviceSettingRepository,
            MechanicServiceSlotRepository serviceSlotRepository,
            NotificationService notificationService,
            MechanicHolidayService holidayService,
            MechanicDailyOverrideService dailyOverrideService,
            UserVehicleRepository userVehicleRepository,
            ReminderCycleService reminderCycleService,
            PaymentRepository paymentRepository,
            com.alvexo.bookingapp.repository.WorkshopBookingSequenceRepository workshopBookingSequenceRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityRepository = availabilityRepository;
        this.mechanicSettingsRepository = mechanicSettingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.serviceSlotRepository = serviceSlotRepository;
        this.notificationService = notificationService;
        this.reminderCycleService = reminderCycleService;
        this.holidayService = holidayService;
        this.dailyOverrideService = dailyOverrideService;
        this.userVehicleRepository = userVehicleRepository;
        this.paymentRepository = paymentRepository;
        this.workshopBookingSequenceRepository = workshopBookingSequenceRepository;
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
     *
     * Rider → workshop routing (RIDER_BOOKING_TO_WORKSHOP.md §4.1):
     *   Tagged channel = RIDER_APP. If the mechanic's jobCardType = AUTO, the
     *   booking is auto-confirmed (and job-carded immediately when autoIssue =
     *   true) so it surfaces on Service Desk Today/Service Week right away.
     *   If jobCardType = MECHANIC (or settings aren't configured yet), it
     *   lands PENDING in the Home dashboard Waiting List for manual approval.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User vehicleUser) {

        // 0. Idempotent replay: a retried submission with the same key returns the
        // original booking instead of creating a duplicate (§4).
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                if (!existing.get().getVehicleUser().getId().equals(vehicleUser.getId())) {
                    throw new BadRequestException("This idempotency key was already used by another request");
                }
                return convertToResponse(existing.get());
            }
        }

        // 1. Validate mechanic
        User mechanic = userRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Selected user is not a mechanic");
        }

        // 2. Validate vehicle, and that it's actually registered to this rider
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!userVehicleRepository.existsByUserAndVehicle(vehicleUser, vehicle)) {
            throw new BadRequestException("This vehicle is not registered to your account");
        }

        // 3. Reject past bookings. Only GENERAL_REPAIR is a real time-slot booking (Level 2 /
        // Advanced — people queue for an exact Slot 1/Slot 2 window), so it alone enforces the
        // configured reporting time as a hard rule: the complete slot date+time must be strictly
        // later than now (equal-to-now is already too late — e.g. at 1:00pm or 1:10pm, a 1:00pm
        // slot is no longer bookable; at 12:50pm it still is). Level 1 GENERAL_SERVICE/
        // EXPRESS_SERVICE treat the configured reporting time as informational only — only the
        // booking DATE can't be in the past; a same-day time that's technically already elapsed
        // is still accepted.
        boolean isSlotBooking = isSlotBooking(request.getServiceType());
        if (isPastBooking(request.getServiceType(), request.getScheduledDateTime(), LocalDateTime.now())) {
            throw new BadRequestException("Bookings cannot be made for past dates");
        }

        // 3b. Reject bookings on a Holiday/Pause date
        rejectIfHolidayOrPause(mechanic, request.getScheduledDateTime().toLocalDate());

        // 4. Validate slot within mechanic's availability template — only meaningful for an
        // actual time-slot booking; a general booking's time isn't matched against working hours.
        if (isSlotBooking) {
            validateSlotWithinAvailability(mechanic, request.getScheduledDateTime());
        }

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

        // 6b. Pickup / drop (RIDER_BOOKING_TO_WORKSHOP.md §3.2) — required addresses when requested
        boolean pickupRequired = Boolean.TRUE.equals(request.getPickupRequired());
        boolean dropRequired = Boolean.TRUE.equals(request.getDropRequired());
        if (pickupRequired && isBlank(request.getPickupAddress())) {
            throw new BadRequestException("pickupAddress is required when pickupRequired is true");
        }
        if (dropRequired && isBlank(request.getDeliveryAddress())) {
            throw new BadRequestException("deliveryAddress is required when dropRequired is true");
        }

        // 6c. Repair time must be one of the workshop's 3 published options — applies to both
        // Instant and Today Approval modes (REPAIR_SLOTS_AND_BOOKING_TIME_SPEC.md §2.3/§4.3).
        if (request.getServiceType() == ServiceType.GENERAL_REPAIR) {
            validateRepairSlotTime(mechanic, request.getScheduledDateTime(), settingsOpt);
        }

        // 6d. Today Approval two-stage flow (§5): rider explicitly requests approval instead
        // of an instant booking. Deliberately skips the capacity check below — that's the
        // whole point of asking the workshop to decide by hand. No capacity is reserved (and
        // no advance is owed) until the workshop accepts and the rider then confirms.
        if (Boolean.TRUE.equals(request.getTodayApprovalRequest())) {
            Booking requestBooking = Booking.builder()
                    .vehicleUser(vehicleUser)
                    .mechanic(mechanic)
                    .vehicle(vehicle)
                    .serviceSetting(serviceSetting)
                    .bookingNumber(generateBookingNumber(mechanic))
                    .scheduledDateTime(request.getScheduledDateTime())
                    .status(BookingStatus.REQUESTED)
                    .bookingType(BookingType.STANDARD)
                    .channel(BookingChannel.RIDER_APP)
                    .serviceType(request.getServiceType())
                    .description(request.getDescription())
                    .estimatedCost(request.getEstimatedCost())
                    .estimatedDurationMinutes(serviceSetting != null
                            ? serviceSetting.getDurationMinutes() : request.getEstimatedDurationMinutes())
                    .advancePaid(BigDecimal.ZERO)
                    .customerNotes(request.getCustomerNotes())
                    .pickupRequired(pickupRequired)
                    .pickupAddress(pickupRequired ? request.getPickupAddress() : null)
                    .dropRequired(dropRequired)
                    .deliveryAddress(dropRequired ? request.getDeliveryAddress() : null)
                    .audioReference(request.getAudioReference())
                    .engineOilReplacement(Boolean.TRUE.equals(request.getEngineOilReplacement()))
                    .idempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                            ? request.getIdempotencyKey() : null)
                    .build();
            requestBooking = bookingRepository.save(requestBooking);

            notificationService.createNotification(
                    mechanic, "New Today Approval Request",
                    "You have a new same-day approval request from " + vehicleUser.getFirstName(),
                    NotificationType.BOOKING_CREATED, "Booking", requestBooking.getId());

            return convertToResponse(requestBooking);
        }

        // 7. Settings-aware capacity checks
        BookingType bookingType = checkCapacityAndResolveBookingType(
                mechanic, serviceSetting, bookingDate, request.getScheduledDateTime(),
                request.getAdvancePaid(), settingsOpt);

        // 8. PESSIMISTIC LOCK — concurrency layer 1. A non-empty result only rejects the booking
        // when the requested time is NOT one of the workshop's published shared times (General/
        // Express reporting time, or a Repair slot) — those are meant to host many bookings at
        // the identical timestamp, bounded by maxVehiclesPerDay/fullDayCapacityHours/repairQty
        // instead of exact-timestamp exclusivity (REPAIR_SLOTS_AND_BOOKING_TIME_SPEC.md).
        List<Booking> conflicts = bookingRepository.findAndLockConflicting(
                mechanic, request.getScheduledDateTime());
        boolean sharedTime = isSharedReportingOrSlotTime(
                mechanic, request.getServiceType(), request.getScheduledDateTime(), settingsOpt);
        if (!conflicts.isEmpty() && !sharedTime) {
            throw new BadRequestException(
                    "This slot is already booked. Please choose a different time.");
        }

        // 8b. A Repair slot's own repairQty caps how many bookings may share that exact slot.
        if (request.getServiceType() == ServiceType.GENERAL_REPAIR) {
            MechanicServiceSlot slot = findMatchingRepairSlot(mechanic, request.getScheduledDateTime());
            if (slot != null && conflicts.size() >= slot.getRepairQty()) {
                throw new BadRequestException(
                        "The selected repair slot is fully booked for this date. "
                                + "Please choose an alternate slot or date.");
            }
        }

        // 9. Route by the mechanic's Job Card Type (RIDER_BOOKING_TO_WORKSHOP.md §4.1):
        //    AUTO       -> auto-confirmed, job card issued immediately (subject to autoIssue).
        //    MECHANIC   -> lands PENDING in the Waiting List for the workshop to approve.
        //    No settings configured yet -> conservative default of PENDING.
        boolean autoConfirm = settingsOpt.map(MechanicSettings::getJobCardType)
                .map(type -> type == JobCardType.AUTO)
                .orElse(false);
        BookingStatus initialStatus = autoConfirm ? BookingStatus.CONFIRMED : BookingStatus.PENDING;
        boolean autoIssue = settingsOpt.map(MechanicSettings::getAutoIssue).orElse(true);

        // 10. Build and save — DB unique constraint is the final guard
        Booking booking = Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .serviceSetting(serviceSetting)
                .bookingNumber(generateBookingNumber(mechanic))
                .scheduledDateTime(request.getScheduledDateTime())
                .status(initialStatus)
                .bookingType(bookingType)
                .channel(BookingChannel.RIDER_APP)
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
                .pickupRequired(pickupRequired)
                .pickupAddress(pickupRequired ? request.getPickupAddress() : null)
                .dropRequired(dropRequired)
                .deliveryAddress(dropRequired ? request.getDeliveryAddress() : null)
                .audioReference(request.getAudioReference())
                .engineOilReplacement(Boolean.TRUE.equals(request.getEngineOilReplacement()))
                .idempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                        ? request.getIdempotencyKey() : null)
                .build();

        if (autoConfirm && autoIssue) {
            booking.setJobCardNumber(generateJobCardNumber(booking));
        }

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_mechanic_slot")) {
                throw new BadRequestException(
                        "This slot was just taken by another booking. Please choose a different time.");
            }
            if (e.getMessage() != null && e.getMessage().contains("uq_bookings_idempotency_key")) {
                // Concurrent retry with the same key raced us — return the winner's booking.
                return bookingRepository.findByIdempotencyKey(booking.getIdempotencyKey())
                        .map(this::convertToResponse)
                        .orElseThrow(() -> e);
            }
            throw e;
        }

        // 11. Notify mechanic
        notificationService.createNotification(
                mechanic,
                autoConfirm ? "New Booking Confirmed" : "New Booking Request",
                (autoConfirm ? "You have a new confirmed " : "You have a new ")
                        + bookingType.name().toLowerCase()
                        + (autoConfirm ? " booking from " : " booking request from ")
                        + vehicleUser.getFirstName(),
                NotificationType.BOOKING_CREATED,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
                .bookingNumber(generateBookingNumber(mechanic))
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
            if (serviceSetting.getDurationMinutes() == null) {
                throw new BadRequestException(
                        "Service '" + serviceSetting.getServiceName() + "' has no configured duration");
            }
            if (fullDayCapacityHours == null) {
                throw new BadRequestException("Mechanic's daily capacity hours are not configured");
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
            if (maxVehiclesPerDay == null) {
                throw new BadRequestException("Mechanic's daily vehicle limit is not configured");
            }
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
        if (newStatus == BookingStatus.REQUESTED || newStatus == BookingStatus.PENDING_PAYMENT
                || newStatus == BookingStatus.SCHEDULED || newStatus == BookingStatus.EXPIRED) {
            throw new BadRequestException(
                    newStatus + " is only set by the Today Approval flow (/workshop-decision, /confirm-request)");
        }

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
            reminderCycleService.ensureCycleForCompletedBooking(booking);
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
     * Rider cancels their own booking (RIDER_BOOKING_TO_WORKSHOP.md §5:
     * "Rider can create, modify, cancel..."). Mirrors the Service Desk
     * cancel flow so the same row is visible as CANCELLED on both sides
     * after refresh (§9 AC-7) — only the ownership check and the actor
     * differ; mechanic-initiated cancellation stays on the Service Desk
     * endpoints, which also flag the reliability adjustment.
     */
    @Transactional
    public BookingResponse cancelBookingAsRider(Long bookingId, User rider, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getVehicleUser().getId().equals(rider.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("A " + booking.getStatus() + " booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setServiceStage(null);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(rider);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getMechanic(),
                "Booking Cancelled",
                "Booking #" + booking.getBookingNumber() + " was cancelled by the customer: " + reason,
                NotificationType.BOOKING_CANCELLED,
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
    public BookingResponse rescheduleBooking(Long bookingId, LocalDateTime newScheduledDateTime, User actor) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        boolean actorIsMechanic = booking.getMechanic().getId().equals(actor.getId());
        boolean actorIsRider = booking.getVehicleUser().getId().equals(actor.getId());
        if (!actorIsMechanic && !actorIsRider) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule a " + booking.getStatus() + " booking");
        }
        if (newScheduledDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot reschedule to a past date/time");
        }

        MechanicSettings settings = mechanicSettingsRepository.findByMechanic(booking.getMechanic()).orElse(null);
        int rescheduleLimit = settings != null ? settings.getRescheduleLimit() : 2;
        LocalTime rescheduleCutoffTime = settings != null ? settings.getRescheduleCutoffTime() : LocalTime.of(17, 0);

        if (booking.getRescheduleCount() >= rescheduleLimit) {
            throw new BadRequestException(
                    "This booking has already been rescheduled " + rescheduleLimit + " time(s), the maximum allowed.");
        }

        LocalDate currentScheduledDate = booking.getScheduledDateTime().toLocalDate();
        if (currentScheduledDate.equals(LocalDate.now().plusDays(1)) && LocalTime.now().isAfter(rescheduleCutoffTime)) {
            throw new BadRequestException(
                    "The reschedule cutoff (" + rescheduleCutoffTime
                            + ") for tomorrow's booking has passed. Please contact the workshop directly.");
        }

        List<Booking> conflicts = bookingRepository.findAndLockConflicting(booking.getMechanic(), newScheduledDateTime);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("The requested time is already booked. Please choose a different time.");
        }

        booking.setScheduledDateTime(newScheduledDateTime);
        booking.setRescheduleCount(booking.getRescheduleCount() + 1);

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("This slot was just taken. Please choose another.");
        }

        // Notify whichever side didn't initiate the change (RIDER_BOOKING_TO_WORKSHOP.md §5:
        // rider can "modify"; a mechanic-initiated reschedule already existed).
        User recipient = actorIsMechanic ? booking.getVehicleUser() : booking.getMechanic();
        notificationService.createNotification(
                recipient,
                "Booking Rescheduled",
                "Booking #" + booking.getBookingNumber() + " has been rescheduled to " + newScheduledDateTime,
                NotificationType.GENERAL,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Workshop proposes an alternate date for an over-capacity booking
     * (RIDER_BOOKING_COLLABORATION spec §3 Seam 5). Unlike {@link #rescheduleBooking},
     * scheduledDateTime does NOT move until the rider explicitly accepts via
     * {@link #respondToProposal}.
     */
    @Transactional
    public BookingResponse proposeDate(Long bookingId, User mechanic, LocalDateTime proposedDateTime) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot propose a date for a " + booking.getStatus() + " booking");
        }
        if (proposedDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot propose a past date/time");
        }

        booking.setProposedDateTime(proposedDateTime);
        booking.setProposalStatus(com.alvexo.bookingapp.model.BookingProposalStatus.PENDING);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getVehicleUser(),
                "Alternate Date Proposed",
                "Your workshop proposed moving booking #" + booking.getBookingNumber() + " to "
                        + proposedDateTime + ". Please accept or decline.",
                NotificationType.GENERAL,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Rider accepts or declines a pending alternate-date proposal. Accepting moves
     * scheduledDateTime to the proposed date; declining cancels the booking (advance
     * refund, if any, is handled by the existing cancellation flow).
     */
    @Transactional
    public BookingResponse respondToProposal(Long bookingId, User rider, boolean accept) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getVehicleUser().getId().equals(rider.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getProposalStatus() != com.alvexo.bookingapp.model.BookingProposalStatus.PENDING
                || booking.getProposedDateTime() == null) {
            throw new BadRequestException("There is no pending date proposal for this booking");
        }

        if (accept) {
            Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(booking.getMechanic());
            List<Booking> conflicts =
                    bookingRepository.findAndLockConflicting(booking.getMechanic(), booking.getProposedDateTime());
            boolean sharedTime = isSharedReportingOrSlotTime(
                    booking.getMechanic(), booking.getServiceType(), booking.getProposedDateTime(), settingsOpt);
            if (!conflicts.isEmpty() && !sharedTime) {
                throw new BadRequestException("The proposed time is no longer available. Please contact the workshop.");
            }
            if (booking.getServiceType() == ServiceType.GENERAL_REPAIR) {
                MechanicServiceSlot slot = findMatchingRepairSlot(booking.getMechanic(), booking.getProposedDateTime());
                if (slot != null && conflicts.size() >= slot.getRepairQty()) {
                    throw new BadRequestException(
                            "The selected repair slot is fully booked for this date. "
                                    + "Please choose an alternate slot or date.");
                }
            }
            booking.setScheduledDateTime(booking.getProposedDateTime());
            booking.setProposalStatus(com.alvexo.bookingapp.model.BookingProposalStatus.ACCEPTED);
            booking.setProposedDateTime(null);
            booking = bookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getMechanic(),
                    "Alternate Date Accepted",
                    "The customer accepted the proposed date for booking #" + booking.getBookingNumber()
                            + ". New date: " + booking.getScheduledDateTime(),
                    NotificationType.GENERAL,
                    "Booking",
                    booking.getId());
        } else {
            booking.setProposalStatus(com.alvexo.bookingapp.model.BookingProposalStatus.DECLINED);
            booking.setProposedDateTime(null);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setServiceStage(null);
            booking.setCancellationReason("Rider declined the proposed alternate date");
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancelledBy(rider);
            booking = bookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getMechanic(),
                    "Alternate Date Declined",
                    "The customer declined the proposed date for booking #" + booking.getBookingNumber()
                            + "; the booking has been cancelled" + (booking.getAdvancePaid() != null
                                    && booking.getAdvancePaid().signum() > 0 ? " and the advance is due for refund." : "."),
                    NotificationType.BOOKING_CANCELLED,
                    "Booking",
                    booking.getId());
        }

        return convertToResponse(booking);
    }

    // ── Today Approval two-stage flow (§5) ────────────────────────────────────

    /**
     * Workshop accepts or rejects a REQUESTED booking. Accepting does not reserve capacity or
     * charge anything yet — it only starts a confirmation window during which the rider must
     * call {@link #confirmRequest}.
     */
    @Transactional
    public com.alvexo.bookingapp.dto.response.WorkshopDecisionResponse workshopDecision(
            Long bookingId, User mechanic, boolean accept) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BadRequestException("Only a REQUESTED booking can be accepted or rejected");
        }

        if (accept) {
            Optional<MechanicSettings> settingsOpt = mechanicSettingsRepository.findByMechanic(mechanic);
            BigDecimal requiredAdvance = !Boolean.TRUE.equals(booking.getPickupRequired())
                    && settingsOpt.map(MechanicSettings::getAdvanceEnabled).orElse(false)
                    ? settingsOpt.get().getAdvanceAmount() : BigDecimal.ZERO;

            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            booking.setRequiredAdvanceAmount(requiredAdvance);
            booking.setConfirmationExpiresAt(
                    LocalDateTime.now().plusMinutes(Constants.TODAY_APPROVAL_CONFIRMATION_WINDOW_MINUTES));
            booking = bookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getVehicleUser(), "Request Accepted",
                    "Booking #" + booking.getBookingNumber() + " was accepted. Please confirm within "
                            + Constants.TODAY_APPROVAL_CONFIRMATION_WINDOW_MINUTES + " minutes.",
                    NotificationType.BOOKING_CONFIRMED, "Booking", booking.getId());

            return new com.alvexo.bookingapp.dto.response.WorkshopDecisionResponse(
                    booking.getId(), booking.getStatus(), booking.getConfirmationExpiresAt(), requiredAdvance);
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getVehicleUser(), "Request Rejected",
                "Booking #" + booking.getBookingNumber() + " was rejected by the workshop.",
                NotificationType.BOOKING_CANCELLED, "Booking", booking.getId());

        return new com.alvexo.bookingapp.dto.response.WorkshopDecisionResponse(
                booking.getId(), booking.getStatus(), null, null);
    }

    /**
     * Rider confirms a workshop-accepted request. If no advance is owed, capacity is reserved
     * and the booking moves straight to SCHEDULED. If an advance is still owed, capacity is
     * deliberately NOT reserved yet — the rider must pay via /payment-intent first, then call
     * this again once the payment is recorded.
     */
    @Transactional
    public com.alvexo.bookingapp.dto.response.ConfirmRequestResponse confirmRequest(Long bookingId, User rider) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getVehicleUser().getId().equals(rider.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }

        expireIfPastDeadline(booking);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "There is nothing to confirm for this booking (status: " + booking.getStatus() + ")");
        }

        BigDecimal required = booking.getRequiredAdvanceAmount() != null
                ? booking.getRequiredAdvanceAmount() : BigDecimal.ZERO;
        BigDecimal paid = booking.getAdvancePaid() != null ? booking.getAdvancePaid() : BigDecimal.ZERO;

        if (required.signum() > 0 && paid.compareTo(required) < 0) {
            return new com.alvexo.bookingapp.dto.response.ConfirmRequestResponse(
                    booking.getId(), booking.getStatus(), true, required.subtract(paid));
        }

        List<Booking> conflicts = bookingRepository.findAndLockConflicting(
                booking.getMechanic(), booking.getScheduledDateTime());
        if (!conflicts.isEmpty()) {
            throw new BusinessRuleException("CAPACITY_UNAVAILABLE",
                    "This slot is no longer available. Please choose a different time.");
        }

        booking.setStatus(BookingStatus.SCHEDULED);
        booking.setConfirmationExpiresAt(null);
        booking.setRequiredAdvanceAmount(null);
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getMechanic(), "Booking Confirmed",
                "Booking #" + booking.getBookingNumber() + " was confirmed by the customer.",
                NotificationType.BOOKING_CONFIRMED, "Booking", booking.getId());

        return new com.alvexo.bookingapp.dto.response.ConfirmRequestResponse(
                booking.getId(), booking.getStatus(), false, BigDecimal.ZERO);
    }

    /**
     * Records an advance payment the rider already completed (e.g. via UPI/Stripe client-side)
     * against a specific booking (MASTER_BACKEND_MVP_INTEGRATION_SPEC.md §4.1). Unlike
     * {@link #confirmRequest}, which gates the Today Approval two-stage flow on an amount already
     * recorded via {@link com.alvexo.bookingapp.service.PaymentService#createBookingPaymentIntent},
     * this is the single call a client makes to both log the payment and move the booking forward
     * in one step.
     */
    @Transactional
    public BookingResponse payAdvance(Long bookingId, User rider, BigDecimal amount, String paymentTransactionId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getVehicleUser().getId().equals(rider.getId())) {
            throw new BadRequestException("You don't have access to this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BadRequestException("A " + booking.getStatus() + " booking cannot accept a payment");
        }

        paymentRepository.save(Payment.builder()
                .user(rider)
                .booking(booking)
                .paymentType(PaymentType.BOOKING_PAYMENT)
                .amount(amount)
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .transactionId(paymentTransactionId)
                .paymentDate(LocalDateTime.now())
                .build());

        booking.setAdvancePaid(amount);
        booking.setRequiredAdvanceAmount(null);
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.SCHEDULED);
            booking.setConfirmationExpiresAt(null);
        } else if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        booking = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getMechanic(),
                "Advance Payment Received",
                "Advance of INR " + amount + " was paid for booking #" + booking.getBookingNumber(),
                NotificationType.GENERAL,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /** Lazily transitions a lapsed PENDING_PAYMENT booking to EXPIRED on access (no background sweep exists). */
    private void expireIfPastDeadline(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getConfirmationExpiresAt() != null
                && booking.getConfirmationExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setConfirmationExpiresAt(null);
            bookingRepository.save(booking);
        }
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

    @Transactional
    public BookingResponse getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getVehicleUser().getId().equals(user.getId())
                && !booking.getMechanic().getId().equals(user.getId())
                && user.getRole() != UserRole.ADMINISTRATOR) {
            throw new BadRequestException("You don't have access to this booking");
        }

        // Lazy expiry — there's no background sweep, so a lapsed Today Approval window is
        // only surfaced (and persisted) the next time the booking is read or acted on.
        expireIfPastDeadline(booking);

        return convertToResponse(booking);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * True for an actual time-slot booking (Level 2 / Advanced — Slot 1/Slot 2, validated
     * against the workshop's published reporting times). Everything else (Level 1 GENERAL_SERVICE,
     * EXPRESS_SERVICE) is a "general" booking whose chosen time is informational only.
     * Package-private (no DB access) so it's directly unit-testable.
     */
    static boolean isSlotBooking(ServiceType serviceType) {
        return serviceType == ServiceType.GENERAL_REPAIR;
    }

    /**
     * Whether {@code scheduledDateTime} is too late to book, given the current server time.
     *
     * Slot bookings (Level 2 / Advanced): the complete slot date+time must be strictly later
     * than {@code now} — equal-to-now is already too late (a 1:00pm slot stops being bookable
     * at exactly 1:00pm, not just after it), so next-day slots are always fine and same-day
     * slots are only fine while still in the future.
     *
     * General bookings (Level 1 GENERAL_SERVICE/EXPRESS_SERVICE): the configured reporting time
     * is informational only — only the booking DATE can't be before today; a same-day time that
     * has technically already elapsed is still accepted.
     *
     * Package-private (no DB access) so it's directly unit-testable.
     */
    static boolean isPastBooking(ServiceType serviceType, LocalDateTime scheduledDateTime, LocalDateTime now) {
        if (isSlotBooking(serviceType)) {
            return !scheduledDateTime.isAfter(now);
        }
        return scheduledDateTime.toLocalDate().isBefore(now.toLocalDate());
    }

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
            // No explicit rows — use the same default-hours fallback that MechanicService uses.
            if (DEFAULT_CLOSED_DAYS.contains(dayOfWeek)) {
                throw new BadRequestException("Mechanic is not available on " + dayOfWeek);
            }
            LocalTime slotTime = scheduledDateTime.toLocalTime();
            if (slotTime.isBefore(DEFAULT_OPEN_START) || slotTime.isAfter(DEFAULT_OPEN_END)) {
                throw new BadRequestException(
                        "The requested time is outside the mechanic's default working hours (" +
                        DEFAULT_OPEN_START + " – " + DEFAULT_OPEN_END + ").");
            }
            return;
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

    private static final DateTimeFormatter TIME_12H =
            DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);

    /**
     * A Repair booking's time must be one of the workshop's 3 published options: the general
     * morning reporting time, or one of its two enabled repair slots
     * (REPAIR_SLOTS_AND_BOOKING_TIME_SPEC.md §4.3).
     */
    private void validateRepairSlotTime(User mechanic, LocalDateTime scheduledDateTime,
            Optional<MechanicSettings> settingsOpt) {
        LocalTime requestedTime = scheduledDateTime.toLocalTime();
        List<MechanicServiceSlot> slots = serviceSlotRepository.findByMechanicOrderBySlotNumberAsc(mechanic)
                .stream().filter(s -> Boolean.TRUE.equals(s.getEnabled())).toList();

        LocalTime reportingTime = settingsOpt.map(MechanicSettings::getServiceReportingTime).orElse(null);
        boolean matchesReportingTime = reportingTime != null && requestedTime.equals(reportingTime);
        boolean matchesSlot = slots.stream().anyMatch(s -> requestedTime.equals(s.getSlotTime()));

        if (!matchesReportingTime && !matchesSlot) {
            List<String> allowed = new java.util.ArrayList<>();
            if (reportingTime != null) allowed.add(reportingTime.format(TIME_12H));
            slots.forEach(s -> allowed.add(s.getSlotTime().format(TIME_12H)));
            throw new BadRequestException(
                    "The requested time is outside the workshop's working hours or repair slots. "
                            + "Allowed times: " + String.join(", ", allowed) + ".");
        }
    }

    /**
     * Whether {@code scheduledDateTime} is one of the workshop's published shared times for
     * {@code serviceType} — the general/express reporting time, or (for Repair) a configured
     * slot time. Bookings at a shared time don't collide with each other; they're bounded by
     * count-based capacity (maxVehiclesPerDay / fullDayCapacityHours / slot repairQty) instead
     * of the exact-timestamp conflict check (REPAIR_SLOTS_AND_BOOKING_TIME_SPEC.md).
     */
    private boolean isSharedReportingOrSlotTime(User mechanic, ServiceType serviceType,
            LocalDateTime scheduledDateTime, Optional<MechanicSettings> settingsOpt) {
        if (settingsOpt.isEmpty()) {
            return false;
        }
        MechanicSettings settings = settingsOpt.get();
        LocalTime requestedTime = scheduledDateTime.toLocalTime();

        if (serviceType == ServiceType.EXPRESS_SERVICE) {
            LocalTime expressTime = settings.getExpressReportingTime() != null
                    ? settings.getExpressReportingTime() : settings.getServiceReportingTime();
            return requestedTime.equals(expressTime);
        }
        if (serviceType == ServiceType.GENERAL_REPAIR) {
            if (requestedTime.equals(settings.getServiceReportingTime())) {
                return true;
            }
            return serviceSlotRepository.findByMechanicOrderBySlotNumberAsc(mechanic).stream()
                    .anyMatch(s -> Boolean.TRUE.equals(s.getEnabled()) && requestedTime.equals(s.getSlotTime()));
        }
        return requestedTime.equals(settings.getServiceReportingTime());
    }

    /**
     * For a Repair booking at a configured Slot 1/2 time, the slot's own repairQty caps how
     * many bookings may share that exact slot — independent of the day-level capacity check.
     * Returns null when the requested time isn't a configured slot (e.g. the general reporting
     * time, which is governed by the day-level check only).
     */
    private MechanicServiceSlot findMatchingRepairSlot(User mechanic, LocalDateTime scheduledDateTime) {
        LocalTime requestedTime = scheduledDateTime.toLocalTime();
        return serviceSlotRepository.findByMechanicOrderBySlotNumberAsc(mechanic).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()) && requestedTime.equals(s.getSlotTime()))
                .findFirst().orElse(null);
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
                .cancelledByName(booking.getCancelledBy() != null
                        ? booking.getCancelledBy().getFirstName() + " " + booking.getCancelledBy().getLastName()
                        : null)
                .canBookAgain(booking.getStatus() == BookingStatus.COMPLETED)
                .pickupRequired(booking.getPickupRequired())
                .pickupAddress(booking.getPickupAddress())
                .dropRequired(booking.getDropRequired())
                .deliveryAddress(booking.getDeliveryAddress())
                .audioReference(booking.getAudioReference())
                .engineOilReplacement(booking.getEngineOilReplacement())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .proposedDateTime(booking.getProposedDateTime())
                .proposalStatus(booking.getProposalStatus())
                .rescheduleCount(booking.getRescheduleCount())
                .build();
    }

    /**
     * Canonical Booking ID series (MASTER_BACKEND_MVP_INTEGRATION_SPEC.md v3 §4.11): builds
     * "{workshopCode} {stateCode} {sequence}" (e.g. "1888 TN 0881") from a per-workshop row in
     * workshop_booking_sequences, creating it on first use. workshopCode is permanently assigned
     * from workshop_code_seq (starts at 1888); stateCode is resolved once from the workshop's
     * onboarded location and then fixed, even if the workshop later edits its address.
     */
    private String generateBookingNumber(User mechanic) {
        WorkshopBookingSequence sequence = workshopBookingSequenceRepository
                .findByMechanicForUpdate(mechanic)
                .orElseGet(() -> createWorkshopBookingSequence(mechanic));

        sequence.setCurrentSequence(sequence.getCurrentSequence() + 1);
        sequence = workshopBookingSequenceRepository.save(sequence);

        return sequence.getWorkshopCode() + " " + sequence.getStateCode() + " "
                + String.format("%04d", sequence.getCurrentSequence());
    }

    private WorkshopBookingSequence createWorkshopBookingSequence(User mechanic) {
        String stateCode = Constants.INDIAN_STATE_CODES.getOrDefault(
                mechanic.getState(), Constants.DEFAULT_STATE_CODE);
        WorkshopBookingSequence sequence = WorkshopBookingSequence.builder()
                .mechanic(mechanic)
                .workshopCode(String.valueOf(workshopBookingSequenceRepository.nextWorkshopCode()))
                .stateCode(stateCode)
                .currentSequence(0)
                .build();
        try {
            return workshopBookingSequenceRepository.save(sequence);
        } catch (DataIntegrityViolationException e) {
            // Lost a race to create the first row for this workshop — the winner already has it.
            return workshopBookingSequenceRepository.findByMechanicForUpdate(mechanic)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Resolves a path {id} that may be either the numeric database id or the canonical
     * bookingNumber string (MASTER_BACKEND_MVP_INTEGRATION_SPEC.md v3 §4.11 "Universal Booking ID
     * Resolution") — lets every /api/bookings/{id}/* endpoint accept either form.
     */
    @Transactional(readOnly = true)
    public Long resolveBookingId(String idOrBookingNumber) {
        try {
            return Long.parseLong(idOrBookingNumber);
        } catch (NumberFormatException e) {
            return bookingRepository.findByBookingNumber(idOrBookingNumber)
                    .map(Booking::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Booking not found: " + idOrBookingNumber));
        }
    }
}
