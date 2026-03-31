package com.alvexo.bookingapp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.BookingRequest;
import com.alvexo.bookingapp.dto.response.BookingResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.DayOfWeek;
import com.alvexo.bookingapp.model.MechanicAvailability;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicAvailabilityRepository;
import com.alvexo.bookingapp.repository.UserRepository;
import com.alvexo.bookingapp.repository.VehicleRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicAvailabilityRepository availabilityRepository;
    private final NotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            MechanicAvailabilityRepository availabilityRepository,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityRepository = availabilityRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a booking with full concurrency protection.
     *
     * Concurrency strategy — two layers:
     *
     * Layer 1 (application): PESSIMISTIC_WRITE lock via findAndLockConflicting().
     *   Acquires an exclusive row-level lock on any existing booking at the same
     *   slot. A concurrent request for the same slot will block here until the
     *   first transaction commits, then see the conflict and be rejected.
     *
     * Layer 2 (database): unique constraint on (mechanic_id, scheduled_date_time).
     *   Final safety net — even if two requests somehow pass the lock check
     *   simultaneously, the DB will reject the second INSERT with a unique
     *   violation, which we catch and convert to a friendly 400 error.
     *
     * Multiple bookings in the same day:
     *   A vehicle user can book multiple mechanics or multiple slots with the
     *   same mechanic on the same day as long as the slots do not overlap.
     *   There is intentionally no per-day limit on the vehicle user side.
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
        if (request.getScheduledDateTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("Bookings cannot be made for past dates");
        }

        // 4. Validate the requested slot falls within mechanic's working hours
        validateSlotWithinAvailability(mechanic, request.getScheduledDateTime());

        // 5. PESSIMISTIC LOCK — acquire exclusive lock on any conflicting booking.
        //    This blocks concurrent requests for the same slot until this transaction completes.
        List<Booking> conflicts = bookingRepository.findAndLockConflicting(
                mechanic, request.getScheduledDateTime());

        if (!conflicts.isEmpty()) {
            throw new BadRequestException(
                    "This slot is already booked. Please choose a different time.");
        }

        // 6. Build and save the booking — DB unique constraint is the final guard
        Booking booking = Booking.builder()
                .vehicleUser(vehicleUser)
                .mechanic(mechanic)
                .vehicle(vehicle)
                .bookingNumber(generateBookingNumber())
                .scheduledDateTime(request.getScheduledDateTime())
                .status(BookingStatus.PENDING)
                .serviceType(request.getServiceType())
                .description(request.getDescription())
                .estimatedCost(request.getEstimatedCost())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .customerNotes(request.getCustomerNotes())
                .build();

        try {
            booking = bookingRepository.save(booking);
        } catch (Exception e) {
            // Unique constraint violation from DB (second layer of protection)
            if (e.getMessage() != null && e.getMessage().contains("uq_mechanic_slot")) {
                throw new BadRequestException(
                        "This slot was just taken by another booking. Please choose a different time.");
            }
            throw e;
        }

        // 7. Notify mechanic
        notificationService.createNotification(
                mechanic,
                "New Booking Request",
                "You have a new booking request from " + vehicleUser.getFirstName(),
                NotificationType.BOOKING_CREATED,
                "Booking",
                booking.getId());

        return convertToResponse(booking);
    }

    /**
     * Validates that the requested scheduledDateTime falls within the mechanic's
     * configured working hours for that day of week.
     */
    private void validateSlotWithinAvailability(User mechanic, LocalDateTime scheduledDateTime) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(scheduledDateTime.getDayOfWeek().name());

        List<MechanicAvailability> rules = availabilityRepository
                .findByMechanicAndDayOfWeek(mechanic, dayOfWeek);

        if (rules.isEmpty()) {
            throw new BadRequestException(
                    "Mechanic is not available on " + dayOfWeek);
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

    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus newStatus, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.REJECTED) {
            if (!booking.getMechanic().getId().equals(user.getId())) {
                throw new BadRequestException("Only the assigned mechanic can confirm/reject bookings");
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
			
		}  catch (DataIntegrityViolationException e) {
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

    private BookingResponse convertToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .vehicleUserId(booking.getVehicleUser().getId())
                .vehicleUserName(booking.getVehicleUser().getFirstName() + " " + booking.getVehicleUser().getLastName())
                .mechanicId(booking.getMechanic().getId())
                .mechanicName(booking.getMechanic().getFirstName() + " " + booking.getMechanic().getLastName())
                .vehicleId(booking.getVehicle().getId())
                .vehicleInfo(booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " " + booking.getVehicle().getYear())
                .scheduledDateTime(booking.getScheduledDateTime())
                .status(booking.getStatus())
                .serviceType(booking.getServiceType())
                .description(booking.getDescription())
                .estimatedCost(booking.getEstimatedCost())
                .actualCost(booking.getActualCost())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .actualDurationMinutes(booking.getActualDurationMinutes())
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
