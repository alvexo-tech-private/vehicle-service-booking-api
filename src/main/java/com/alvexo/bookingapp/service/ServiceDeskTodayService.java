package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.response.BulkStatusResultResponse;
import com.alvexo.bookingapp.dto.response.NextJobCardNumberResponse;
import com.alvexo.bookingapp.dto.response.ServiceBookingInfoResponse;
import com.alvexo.bookingapp.dto.response.ServiceCancelResponse;
import com.alvexo.bookingapp.dto.response.ServiceTodayResponse;
import com.alvexo.bookingapp.dto.response.ServiceVehicleResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.ServiceDeskStage;
import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;
import com.alvexo.bookingapp.util.ServiceDeskMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Backs the Service Desk "Today" tab (SERVICE_DESK_API_SPEC.md §1). */
@Service
public class ServiceDeskTodayService {

    /** §1.4 status transition table for the generic PUT .../status endpoint (Arrive/Cancel have dedicated endpoints). */
    private static final Set<ServiceWorkspaceStatus> GENERIC_ENDPOINT_TARGETS =
            EnumSet.of(ServiceWorkspaceStatus.PENDING, ServiceWorkspaceStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final MechanicConfigurationSettingsRepository configurationSettingsRepository;
    private final NotificationService notificationService;

    public ServiceDeskTodayService(BookingRepository bookingRepository,
                                    UserVehicleRepository userVehicleRepository,
                                    MechanicConfigurationSettingsRepository configurationSettingsRepository,
                                    NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userVehicleRepository = userVehicleRepository;
        this.configurationSettingsRepository = configurationSettingsRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public ServiceTodayResponse getToday(User mechanic, LocalDate date) {
        rejectFutureDate(date);

        List<Booking> bookings = bookingRepository.findTodayWorkspaceBookings(mechanic, date);

        List<ServiceVehicleResponse> carryOver = new ArrayList<>();
        List<ServiceVehicleResponse> today = new ArrayList<>();
        List<ServiceVehicleResponse> cancelled = new ArrayList<>();

        long completed = 0, arrived = 0, pending = 0, scheduled = 0;

        for (Booking booking : bookings) {
            ServiceVehicleResponse row = this.toVehicleResponse(booking);
            boolean isCancelled = row.getStatus() == ServiceWorkspaceStatus.CANCELLED;
            boolean isCarryOver = Boolean.TRUE.equals(booking.getIsCarryOver()) && !isCancelled;

            if (isCancelled) {
                cancelled.add(row);
            } else if (isCarryOver) {
                carryOver.add(row);
            } else {
                today.add(row);
                switch (row.getStatus()) {
                    case COMPLETED -> completed++;
                    case ARRIVED -> arrived++;
                    case PENDING -> pending++;
                    case SCHEDULED -> scheduled++;
                    default -> { /* CANCELLED handled above */ }
                }
            }
        }

        return ServiceTodayResponse.builder()
                .carryOver(carryOver)
                .today(today)
                .cancelled(cancelled)
                .completed(completed)
                .arrived(arrived)
                .pending(pending)
                .scheduled(scheduled)
                .build();
    }

    @Transactional(readOnly = true)
    public ServiceBookingInfoResponse getBookingInfo(User mechanic, String bookingId) {
        Booking booking = resolveBooking(mechanic, bookingId);
        String registrationNumber = resolveRegistrationNumber(booking);

        return ServiceBookingInfoResponse.builder()
                .bookingId(booking.getBookingNumber())
                .ownerName(ServiceDeskMapper.fullName(
                        booking.getVehicleUser().getFirstName(), booking.getVehicleUser().getLastName()))
                .mobile(booking.getVehicleUser().getMobileNumber())
                .registrationNumber(registrationNumber)
                .vehicleMake(booking.getVehicle().getMake())
                .vehicleModel(booking.getVehicle().getModel())
                .serviceType(ServiceDeskMapper.serviceTypeAbbr(booking))
                .pickupRequired(booking.getPickupRequired())
                .dropRequired(booking.getDropRequired())
                .reportingTime(ServiceDeskMapper.formatReportingTime(booking.getScheduledDateTime()))
                .customerRemarks(booking.getCustomerNotes())
                .advancePaid(booking.getAdvancePaid())
                .build();
    }

    @Transactional
    public ServiceVehicleResponse changeStatus(User mechanic, String bookingId, ServiceWorkspaceStatus target) {
        if (!GENERIC_ENDPOINT_TARGETS.contains(target)) {
            throw new BadRequestException(
                    "Use /arrive to move to Arrived and /cancel to move to Cancelled");
        }

        Booking booking = resolveBooking(mechanic, bookingId);
        ServiceWorkspaceStatus current = ServiceDeskMapper.toWorkspaceStatus(booking);
        if (!allowedNextStatuses(current).contains(target)) {
            throw new BadRequestException("Cannot move a " + current + " booking to " + target);
        }

        applyStatus(booking, target);
        booking = bookingRepository.save(booking);
        return toVehicleResponse(booking);
    }

    @Transactional
    public ServiceVehicleResponse arrive(User mechanic, String bookingId, String jobCardNumber) {
        Booking booking = resolveBooking(mechanic, bookingId);
        ServiceWorkspaceStatus current = ServiceDeskMapper.toWorkspaceStatus(booking);
        if (current != ServiceWorkspaceStatus.SCHEDULED) {
            throw new BadRequestException("Only a Scheduled booking can be marked Arrived");
        }

        Long currentBookingDbId = booking.getId();
        bookingRepository.findByJobCardNumber(jobCardNumber).ifPresent(existing -> {
            if (!existing.getId().equals(currentBookingDbId)) {
                throw new BadRequestException(
                        "Duplicate Job Card / " + jobCardNumber + " is already in use.");
            }
        });

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setServiceStage(ServiceDeskStage.ARRIVED);
        booking.setJobCardNumber(jobCardNumber);

        try {
            booking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(
                    "Duplicate Job Card / " + jobCardNumber + " is already in use.");
        }

        return toVehicleResponse(booking);
    }

    @Transactional
    public ServiceCancelResponse cancel(User mechanic, String bookingId, String message) {
        Booking booking = resolveBooking(mechanic, bookingId);
        ServiceWorkspaceStatus current = ServiceDeskMapper.toWorkspaceStatus(booking);
        if (!allowedNextStatuses(current).contains(ServiceWorkspaceStatus.CANCELLED)) {
            throw new BadRequestException("A " + current + " booking cannot be cancelled");
        }

        var reliabilityAdjustment = applyCancellation(booking, mechanic, message);
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

    @Transactional
    public BulkStatusResultResponse bulkStatus(User mechanic, List<String> bookingIds, ServiceWorkspaceStatus target) {
        if (target != ServiceWorkspaceStatus.COMPLETED && target != ServiceWorkspaceStatus.PENDING) {
            throw new BadRequestException("Bulk status only supports Completed or Pending as a target");
        }

        List<BulkStatusResultResponse.Item> results = bookingIds.stream()
                .map(id -> applyBulkItem(mechanic, id, target))
                .collect(Collectors.toList());

        return BulkStatusResultResponse.builder().results(results).build();
    }

    @Transactional(readOnly = true)
    public NextJobCardNumberResponse nextJobCardNumber(User mechanic) {
        MechanicConfigurationSettings config = configurationSettingsRepository.findByMechanic(mechanic)
                .orElseGet(() -> MechanicConfigurationSettings.builder().build());
        List<String> existing = bookingRepository.findJobCardNumbersByMechanic(mechanic);
        String next = ServiceDeskMapper.computeNextJobCardNumber(config, existing);
        return NextJobCardNumberResponse.builder().jobCardNumber(next).build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BulkStatusResultResponse.Item applyBulkItem(User mechanic, String bookingId, ServiceWorkspaceStatus target) {
        Booking booking;
        try {
            booking = resolveBooking(mechanic, bookingId);
        } catch (ResourceNotFoundException e) {
            return BulkStatusResultResponse.Item.builder()
                    .bookingId(bookingId).applied(false).message("Booking not found").build();
        }

        ServiceWorkspaceStatus current = ServiceDeskMapper.toWorkspaceStatus(booking);
        if (!allowedNextStatuses(current).contains(target)) {
            return BulkStatusResultResponse.Item.builder()
                    .bookingId(bookingId).applied(false).resultingStatus(current)
                    .message("Cannot move a " + current + " booking to " + target).build();
        }

        applyStatus(booking, target);
        bookingRepository.save(booking);
        return BulkStatusResultResponse.Item.builder()
                .bookingId(bookingId).applied(true).resultingStatus(target).build();
    }

    private void applyStatus(Booking booking, ServiceWorkspaceStatus target) {
        switch (target) {
            case PENDING -> {
                booking.setStatus(BookingStatus.IN_PROGRESS);
                booking.setServiceStage(ServiceDeskStage.PENDING);
            }
            case COMPLETED -> {
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setServiceStage(null);
                booking.setCompletedAt(LocalDateTime.now());
            }
            default -> throw new IllegalArgumentException("Unsupported target status: " + target);
        }
    }

    /** Shared by Today (§1.7) and Service Week (§2.5) cancellation. */
    java.math.BigDecimal applyCancellation(Booking booking, User actor, String message) {
        MechanicConfigurationSettings config = configurationSettingsRepository.findByMechanic(booking.getMechanic())
                .orElse(null);
        LocalTime cutoff = config != null ? config.getRescheduleCutoffTime() : null;
        var reliabilityAdjustment = ServiceDeskMapper.computeReliabilityAdjustment(cutoff, LocalTime.now());

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setServiceStage(null);
        booking.setCancellationMessage(message);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(actor);
        booking.setReliabilityAdjustmentAmount(reliabilityAdjustment);
        return reliabilityAdjustment;
    }

    /** §1.4 transition table. */
    static Set<ServiceWorkspaceStatus> allowedNextStatuses(ServiceWorkspaceStatus current) {
        return switch (current) {
            case SCHEDULED -> EnumSet.of(ServiceWorkspaceStatus.ARRIVED, ServiceWorkspaceStatus.PENDING,
                    ServiceWorkspaceStatus.CANCELLED);
            case ARRIVED -> EnumSet.of(ServiceWorkspaceStatus.PENDING, ServiceWorkspaceStatus.COMPLETED);
            case PENDING -> EnumSet.of(ServiceWorkspaceStatus.COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(ServiceWorkspaceStatus.class);
        };
    }

    Booking resolveBooking(User mechanic, String bookingId) {
        Booking booking = bookingRepository.findByBookingNumberAndMechanic(bookingId, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        return booking;
    }

    private void rejectFutureDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Future dates are not available on this tab");
        }
    }

    ServiceVehicleResponse toVehicleResponse(Booking booking) {
        ServiceWorkspaceStatus status = ServiceDeskMapper.toWorkspaceStatus(booking);
        return ServiceVehicleResponse.builder()
                .bookingId(booking.getBookingNumber())
                .jobCardNumber(booking.getJobCardNumber())
                .make(booking.getVehicle().getMake())
                .model(booking.getVehicle().getModel())
                .regnLast4(ServiceDeskMapper.regnLast4(resolveRegistrationNumber(booking)))
                .serviceType(ServiceDeskMapper.serviceTypeAbbr(booking))
                .status(status)
                .isCarryOver(booking.getIsCarryOver())
                .cancellationMessage(status == ServiceWorkspaceStatus.CANCELLED ? booking.getCancellationMessage() : null)
                .build();
    }

    String resolveRegistrationNumber(Booking booking) {
        return userVehicleRepository.findByUserAndVehicle(booking.getVehicleUser(), booking.getVehicle())
                .map(uv -> uv.getRegistrationNumber())
                .orElse(null);
    }
}
