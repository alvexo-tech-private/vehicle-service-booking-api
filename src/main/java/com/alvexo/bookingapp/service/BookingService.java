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
import com.alvexo.bookingapp.dto.request.MechanicCreatedBookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicAvailabilityRepository availabilityRepository;
    private final MechanicSettingsRepository mechanicSettingsRepository;
    private final MechanicServiceSettingRepository serviceSettingRepository;
    private final MechanicServiceSlotRepository slotRepository;
    private final JobCardSequenceRepository jobCardSequenceRepository;
    private final NotificationService notificationService;
    private final MechanicSettingsService mechanicSettingsService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            MechanicAvailabilityRepository availabilityRepository,
            MechanicSettingsRepository mechanicSettingsRepository,
            MechanicServiceSettingRepository serviceSettingRepository,
            MechanicServiceSlotRepository slotRepository,
            JobCardSequenceRepository jobCardSequenceRepository,
            NotificationService notificationService,
            MechanicSettingsService mechanicSettingsService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityRepository = availabilityRepository;
        this.mechanicSettingsRepository = mechanicSettingsRepository;
        this.serviceSettingRepository = serviceSettingRepository;
        this.slotRepository = slotRepository;
        this.jobCardSequenceRepository = jobCardSequenceRepository;
        this.notificationService = notificationService;
        this.mechanicSettingsService = mechanicSettingsService;
    }

    // ── Rider booking (POST /api/bookings) ───────────────────────────────────

    @Transactional
    public BookingResponse createBooking(BookingRequest request, User vehicleUser) {

        User mechanic = userRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found"));
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Selected user is not a mechanic");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (request.getScheduledDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Bookings cannot be made for past dates");
        }

        validateSlotWithinAvailability(mechanic, request.getScheduledDateTime());

        MechanicSettings settings = mechanicSettingsRepository.findByMechanic(mechanic)
                .orElse(null);

        LocalDate bookingDate = request.getScheduledDateTime().toLocalDate();

        MechanicServiceSetting serviceSetting = resolveServiceSetting(request.getServiceSettingId(), mechanic);

        // Concurrency layer 1: pessimistic lock
        List<Booking> conflicts = bookingRepository.findAndLockConflicting(
                mechanic, request.getScheduledDateTime());
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("This slot is already booked. Please choose a different time.");
        }

        // Determine booking type (express vs standard)
        BookingType bookingType = resolveBookingType(settings, serviceSetting, request.getScheduledDateTime());

        // Advance payment check
        validateAdvancePayment(settings, request.getAdvancePaid());

        // Build base booking
        Booking booking = buildBaseBooking(vehicleUser, mechanic, vehicle, serviceSetting,
                request, bookingType);
        booking.setBookingSource(BookingSource.RIDER_APP);

        // Branch by job card type
        if (settings == null) {
            booking.setStatus(BookingStatus.PENDING);
        } else {
            applyJobCardTypeLogic(booking, settings, serviceSetting, bookingDate);
        }

        booking = saveBookingWithConflictGuard(booking);

        notificationService.createNotification(
                mechanic, "New Booking Request",
                "You have a new booking request from " + vehicleUser.getFirstName(),
                NotificationType.BOOKING_CREATED, "Booking", booking.getId());

        return convertToResponse(booking);
    }

    // ── Walk-in booking (POST /api/bookings/walk-in) ─────────────────────────

    @Transactional
    public BookingResponse createWalkInBooking(MechanicCreatedBookingRequest request, User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only mechanics can create walk-in bookings");
        }

        MechanicSettings settings = mechanicSettingsRepository.findByMechanic(mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not configured"));

        if (settings.getJobCardType() != JobCardType.TYPE_3 && settings.getJobCardType() != JobCardType.TYPE_4) {
            throw new BadRequestException("Walk-in bookings are only available for TYPE_3 and TYPE_4 mechanics");
        }

        LocalDate bookingDate = request.getScheduledDateTime().toLocalDate();

        MechanicServiceSetting serviceSetting = resolveServiceSetting(
                request.getServiceSettingId(), mechanic);

        // Global capacity check
        BigDecimal effectiveCapacity = mechanicSettingsService.getEffectiveCapacityHours(settings, bookingDate);
        long totalMinutes = bookingRepository.sumAllBookedMinutesForDate(mechanic.getId(), bookingDate);
        int duration = serviceSetting != null ? serviceSetting.getDurationMinutes() : 60;

        if (effectiveCapacity != null && (totalMinutes + duration) > effectiveCapacity.multiply(BigDecimal.valueOf(60)).longValue()) {
            throw new BadRequestException("Quota Limit Exceeded — daily capacity is full for " + bookingDate);
        }

        // Link to existing user if mobile matches
        User linkedUser = null;
        if (request.getCustomerMobile() != null) {
            linkedUser = userRepository.findByMobileNumber(request.getCustomerMobile()).orElse(null);
        }

        Booking booking = Booking.builder()
                .vehicleUser(linkedUser)
                .mechanic(mechanic)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(request.getScheduledDateTime())
                .status(BookingStatus.CONFIRMED)
                .bookingType(BookingType.STANDARD)
                .serviceType(ServiceType.MAINTENANCE)
                .description(request.getDescription())
                .serviceSetting(serviceSetting)
                .estimatedDurationMinutes(duration)
                .bookingSource(BookingSource.WALK_IN)
                .allocationResult(AllocationResult.AUTO_CONFIRMED)
                .walkInCustomerName(request.getCustomerName())
                .walkInCustomerMobile(request.getCustomerMobile())
                .walkInVehicleDescription(request.getVehicleDescription())
                .customerNotes(request.getCustomerNotes())
                .build();

        booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
        booking = bookingRepository.save(booking);

        return convertToResponse(booking);
    }

    // ── Status update ────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus newStatus, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.REJECTED) {
            if (!booking.getMechanic().getId().equals(user.getId())) {
                throw new BadRequestException("Only the assigned mechanic can confirm/reject bookings");
            }
        }

        if (newStatus == BookingStatus.CONFIRMED && booking.getJobCardNumber() == null) {
            MechanicSettings settings = mechanicSettingsRepository.findByMechanic(booking.getMechanic())
                    .orElse(null);
            if (settings != null) {
                booking.setJobCardNumber(generateJobCardNumber(settings,
                        booking.getScheduledDateTime().toLocalDate()));
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

        if (recipient != null) {
            notificationService.createNotification(
                    recipient, "Booking Status Updated",
                    "Booking #" + booking.getBookingNumber() + " status: " + newStatus,
                    NotificationType.valueOf("BOOKING_" + newStatus),
                    "Booking", booking.getId());
        }

        return convertToResponse(booking);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<BookingResponse> getUserBookings(User user, Pageable pageable) {
        if (user.getRole() == UserRole.MECHANIC) {
            return bookingRepository.findByMechanic(user, pageable).map(this::convertToResponse);
        }
        return bookingRepository.findByVehicleUser(user, pageable).map(this::convertToResponse);
    }

    public BookingResponse getBookingById(Long id, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        boolean isOwner = (booking.getVehicleUser() != null && booking.getVehicleUser().getId().equals(user.getId()))
                || booking.getMechanic().getId().equals(user.getId())
                || user.getRole() == UserRole.ADMINISTRATOR;

        if (!isOwner) {
            throw new BadRequestException("You don't have access to this booking");
        }

        return convertToResponse(booking);
    }

    // ── Job card type logic ──────────────────────────────────────────────────

    private void applyJobCardTypeLogic(Booking booking, MechanicSettings settings,
                                        MechanicServiceSetting serviceSetting,
                                        LocalDate bookingDate) {
        switch (settings.getJobCardType()) {
            case TYPE_1 -> applyType1(booking, settings, bookingDate);
            case TYPE_2 -> applyType2(booking, settings, serviceSetting, bookingDate);
            case TYPE_3 -> applyType3(booking, settings, serviceSetting, bookingDate);
            case TYPE_4 -> applyType4(booking, settings, serviceSetting, bookingDate);
        }
    }

    private void applyType1(Booking booking, MechanicSettings settings, LocalDate bookingDate) {
        Integer effectiveMax = mechanicSettingsService.getEffectiveMaxVehicles(settings, bookingDate);
        long bookedCount = bookingRepository.countActiveBookingsForMechanicOnDate(
                booking.getMechanic(), bookingDate);

        if (bookedCount >= effectiveMax) {
            throw new BadRequestException("Daily booking limit reached (" + effectiveMax + " vehicles)");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAllocationResult(AllocationResult.AUTO_CONFIRMED);
        booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
    }

    private void applyType2(Booking booking, MechanicSettings settings,
                             MechanicServiceSetting serviceSetting, LocalDate bookingDate) {
        if (serviceSetting == null) {
            throw new BadRequestException("serviceSettingId is required for TYPE_2 mechanics");
        }

        // Per-service cap
        if (serviceSetting.getMaxSlotsPerDay() != null) {
            long serviceCount = bookingRepository.countBookingsForServiceOnDate(
                    serviceSetting.getId(), bookingDate);
            if (serviceCount >= serviceSetting.getMaxSlotsPerDay()) {
                throw new BadRequestException("Service '" + serviceSetting.getServiceName()
                        + "' is unavailable for this date");
            }
        }

        // Overall hour cap
        if (settings.getTotalDailyCapacityHours() != null) {
            long totalMinutes = bookingRepository.sumAllBookedMinutesForDate(
                    booking.getMechanic().getId(), bookingDate);
            long capMinutes = settings.getTotalDailyCapacityHours()
                    .multiply(BigDecimal.valueOf(60)).longValue();
            if ((totalMinutes + serviceSetting.getDurationMinutes()) > capMinutes) {
                throw new BadRequestException("Daily capacity exceeded for " + bookingDate);
            }
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAllocationResult(AllocationResult.AUTO_CONFIRMED);
        booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
    }

    private void applyType3(Booking booking, MechanicSettings settings,
                             MechanicServiceSetting serviceSetting, LocalDate bookingDate) {
        if (serviceSetting == null) {
            throw new BadRequestException("serviceSettingId is required for TYPE_3 mechanics");
        }

        BigDecimal effectiveCapacity = mechanicSettingsService.getEffectiveCapacityHours(settings, bookingDate);
        int duration = serviceSetting.getDurationMinutes();
        Long mechanicId = booking.getMechanic().getId();

        // Global safeguard
        long totalMinutes = bookingRepository.sumAllBookedMinutesForDate(mechanicId, bookingDate);
        if (effectiveCapacity != null && (totalMinutes + duration) > effectiveCapacity.multiply(BigDecimal.valueOf(60)).longValue()) {
            throw new BadRequestException("Quota Limit Exceeded — daily capacity is full");
        }

        // Auto-allocation check
        if (Boolean.TRUE.equals(settings.getAutoAllocationEnabled())
                && settings.getAutoAllocationCapacityHours() != null) {

            long autoMinutes = bookingRepository.sumBookedMinutesByAllocationResult(
                    mechanicId, bookingDate, AllocationResult.AUTO_CONFIRMED);
            long autoCapMinutes = settings.getAutoAllocationCapacityHours()
                    .multiply(BigDecimal.valueOf(60)).longValue();

            if ((autoMinutes + duration) <= autoCapMinutes) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setAllocationResult(AllocationResult.AUTO_CONFIRMED);
                booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
            } else {
                booking.setStatus(BookingStatus.PENDING);
                booking.setAllocationResult(AllocationResult.MANUAL_REVIEW);
            }
        } else {
            booking.setStatus(BookingStatus.PENDING);
            booking.setAllocationResult(AllocationResult.MANUAL_REVIEW);
        }
    }

    private void applyType4(Booking booking, MechanicSettings settings,
                             MechanicServiceSetting serviceSetting, LocalDate bookingDate) {
        if (serviceSetting == null) {
            throw new BadRequestException("serviceSettingId is required for TYPE_4 mechanics");
        }

        Long mechanicId = booking.getMechanic().getId();
        int duration = serviceSetting.getDurationMinutes();

        // Check if service category matches a restricted slot
        List<MechanicServiceSlot> matchingSlots = slotRepository
                .findByMechanicSettingsAndRestrictedCategoryAndIsEnabledTrue(
                        settings, serviceSetting.getCategory());

        // Filter by applicable day
        java.time.DayOfWeek dayOfWeek = bookingDate.getDayOfWeek();
        MechanicServiceSlot targetSlot = matchingSlots.stream()
                .filter(s -> s.isApplicableOn(dayOfWeek))
                .findFirst().orElse(null);

        if (targetSlot != null) {
            // ── SLOT BOOKING PATH ────────────────────────────────────────
            long slotBookedCount = bookingRepository.countSlotBookingsForDate(
                    targetSlot.getId(), bookingDate);

            if (slotBookedCount >= targetSlot.getMaxVehicleQty()) {
                throw new BadRequestException("Slot capacity full for "
                        + targetSlot.getRestrictedCategory() + " on " + bookingDate);
            }

            booking.setServiceSlot(targetSlot);

            long autoCount = bookingRepository.countAutoConfirmedSlotBookingsForDate(
                    targetSlot.getId(), bookingDate);

            if (autoCount < targetSlot.getAutoAllocationQty()) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setAllocationResult(AllocationResult.AUTO_CONFIRMED);
                booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
            } else {
                booking.setStatus(BookingStatus.PENDING);
                booking.setAllocationResult(AllocationResult.MANUAL_REVIEW);
            }
        } else {
            // ── GENERAL BOOKING PATH (TYPE_3 with slot deduction) ────────
            BigDecimal effectiveCapacity = mechanicSettingsService.getEffectiveCapacityHours(settings, bookingDate);
            long slotConsumedMinutes = bookingRepository.sumSlotConsumedMinutesForDate(mechanicId, bookingDate);

            long availableGeneralMinutes = effectiveCapacity != null
                    ? effectiveCapacity.multiply(BigDecimal.valueOf(60)).longValue() - slotConsumedMinutes
                    : Long.MAX_VALUE;

            long totalGeneralMinutes = bookingRepository.sumAllBookedMinutesForDate(mechanicId, bookingDate)
                    - slotConsumedMinutes;

            if ((totalGeneralMinutes + duration) > availableGeneralMinutes) {
                throw new BadRequestException("Quota Limit Exceeded — general capacity is full");
            }

            // Auto-allocation within general pool
            if (Boolean.TRUE.equals(settings.getAutoAllocationEnabled())
                    && settings.getAutoAllocationCapacityHours() != null) {

                long autoMinutes = bookingRepository.sumBookedMinutesByAllocationResult(
                        mechanicId, bookingDate, AllocationResult.AUTO_CONFIRMED);
                long autoCapMinutes = settings.getAutoAllocationCapacityHours()
                        .multiply(BigDecimal.valueOf(60)).longValue();

                if ((autoMinutes + duration) <= autoCapMinutes) {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    booking.setAllocationResult(AllocationResult.AUTO_CONFIRMED);
                    booking.setJobCardNumber(generateJobCardNumber(settings, bookingDate));
                } else {
                    booking.setStatus(BookingStatus.PENDING);
                    booking.setAllocationResult(AllocationResult.MANUAL_REVIEW);
                }
            } else {
                booking.setStatus(BookingStatus.PENDING);
                booking.setAllocationResult(AllocationResult.MANUAL_REVIEW);
            }
        }
    }

    // ── Job card number generation (concurrency-safe) ────────────────────────

    private String generateJobCardNumber(MechanicSettings settings, LocalDate date) {
        String prefix = settings.getJobCardSerialPrefix();
        String datePart = String.format("%02d%02d%02d",
                date.getYear() % 100, date.getMonthValue(), date.getDayOfMonth());

        Optional<JobCardSequence> seqOpt = jobCardSequenceRepository
                .findAndLockByMechanicSettingsAndDate(settings, date);

        JobCardSequence seq;
        if (seqOpt.isPresent()) {
            seq = seqOpt.get();
            seq.setLastSequence(seq.getLastSequence() + 1);
        } else {
            seq = JobCardSequence.builder()
                    .mechanicSettings(settings)
                    .sequenceDate(date)
                    .lastSequence(1)
                    .build();
        }
        jobCardSequenceRepository.save(seq);

        return prefix + datePart + String.format("%02d", seq.getLastSequence());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private MechanicServiceSetting resolveServiceSetting(Long serviceSettingId, User mechanic) {
        if (serviceSettingId == null) return null;

        MechanicServiceSetting setting = serviceSettingRepository.findById(serviceSettingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service setting not found"));
        if (!setting.getMechanic().getId().equals(mechanic.getId())) {
            throw new BadRequestException("Service does not belong to the selected mechanic");
        }
        if (!Boolean.TRUE.equals(setting.getIsActive())) {
            throw new BadRequestException("Selected service is no longer available");
        }
        return setting;
    }

    private BookingType resolveBookingType(MechanicSettings settings,
                                           MechanicServiceSetting serviceSetting,
                                           LocalDateTime scheduledDateTime) {
        if (settings != null
                && Boolean.TRUE.equals(settings.getReserveCapacity())
                && settings.getExpressReportingTime() != null
                && scheduledDateTime.toLocalTime().isBefore(settings.getExpressReportingTime())) {

            if (serviceSetting != null && !Boolean.TRUE.equals(serviceSetting.getIsExpressEligible())) {
                throw new BadRequestException("Service '" + serviceSetting.getServiceName()
                        + "' is not eligible for express booking");
            }
            return BookingType.EXPRESS;
        }
        return BookingType.STANDARD;
    }

    private void validateAdvancePayment(MechanicSettings settings, BigDecimal advancePaid) {
        if (settings != null && Boolean.TRUE.equals(settings.getAdvanceEnabled())
                && settings.getAdvanceAmount() != null) {
            BigDecimal paid = advancePaid != null ? advancePaid : BigDecimal.ZERO;
            if (paid.compareTo(settings.getAdvanceAmount()) < 0) {
                throw new BadRequestException("Advance payment of INR "
                        + settings.getAdvanceAmount() + " is required");
            }
        }
    }

    private Booking buildBaseBooking(User vehicleUser, User mechanic, Vehicle vehicle,
                                      MechanicServiceSetting serviceSetting,
                                      BookingRequest request, BookingType bookingType) {
        return Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .serviceSetting(serviceSetting)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(request.getScheduledDateTime())
                .bookingType(bookingType)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDurationMinutes(serviceSetting != null
                        ? serviceSetting.getDurationMinutes()
                        : request.getEstimatedDurationMinutes())
                .advancePaid(request.getAdvancePaid() != null
                        ? request.getAdvancePaid() : BigDecimal.ZERO)
                .customerNotes(request.getCustomerNotes())
                .build();
    }

    private Booking saveBookingWithConflictGuard(Booking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_mechanic_slot")) {
                throw new BadRequestException(
                        "This slot was just taken by another booking. Please choose a different time.");
            }
            throw e;
        }
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

    private BookingResponse convertToResponse(Booking booking) {
        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .jobCardNumber(booking.getJobCardNumber())
                .mechanicId(booking.getMechanic().getId())
                .mechanicName(booking.getMechanic().getFirstName() + " " +
                              booking.getMechanic().getLastName())
                .scheduledDateTime(booking.getScheduledDateTime())
                .status(booking.getStatus())
                .bookingType(booking.getBookingType())
                .serviceType(booking.getServiceType())
                .description(booking.getDescription())
                .estimatedCost(booking.getEstimatedCost())
                .actualCost(booking.getActualCost())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .actualDurationMinutes(booking.getActualDurationMinutes())
                .advancePaid(booking.getAdvancePaid())
                .bookingSource(booking.getBookingSource())
                .allocationResult(booking.getAllocationResult())
                .serviceSlotId(booking.getServiceSlot() != null ? booking.getServiceSlot().getId() : null)
                .walkInCustomerName(booking.getWalkInCustomerName())
                .walkInCustomerMobile(booking.getWalkInCustomerMobile())
                .walkInVehicleDescription(booking.getWalkInVehicleDescription())
                .mechanicNotes(booking.getMechanicNotes())
                .customerNotes(booking.getCustomerNotes())
                .cancellationReason(booking.getCancellationReason())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt());

        if (booking.getVehicleUser() != null) {
            builder.vehicleUserId(booking.getVehicleUser().getId())
                   .vehicleUserName(booking.getVehicleUser().getFirstName() + " " +
                                    booking.getVehicleUser().getLastName());
        }

        if (booking.getVehicle() != null) {
            builder.vehicleId(booking.getVehicle().getId())
                   .vehicleInfo(booking.getVehicle().getMake() + " " +
                                booking.getVehicle().getModel() + " " +
                                booking.getVehicle().getYear());
        }

        if (booking.getServiceSetting() != null) {
            builder.serviceSettingId(booking.getServiceSetting().getId())
                   .serviceSettingName(booking.getServiceSetting().getServiceName());
        }

        return builder.build();
    }

    private String generateBookingNumber() {
        return "BKG" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
