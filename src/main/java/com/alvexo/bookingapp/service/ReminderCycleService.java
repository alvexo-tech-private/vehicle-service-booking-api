package com.alvexo.bookingapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvexo.bookingapp.dto.request.ReminderNotifyRequest;
import com.alvexo.bookingapp.dto.response.ReminderNotifyItemResult;
import com.alvexo.bookingapp.dto.response.ReminderNotifyResponse;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingStatus;
import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.NotificationType;
import com.alvexo.bookingapp.model.ReminderCycle;
import com.alvexo.bookingapp.model.ReminderCycleStatus;
import com.alvexo.bookingapp.model.ReminderNotifyStage;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.Vehicle;
import com.alvexo.bookingapp.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.bookingapp.repository.ReminderCycleRepository;
import com.alvexo.bookingapp.repository.UserVehicleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service Reminder Scheduling (BACKEND_REQUIREMENTS_SERVICE_REMINDER_128.md). A cycle's
 * {@code scheduledServiceDate} is fixed at creation and never recalculated by reminder
 * delivery; {@code secondReminderScheduledAt} is always derived from the actual
 * {@code firstReminderSentAt}, never from the due date or a planned time.
 *
 * There is no background sweep here — no scheduler infrastructure exists in this codebase
 * yet, so the second reminder becomes sendable once its window has passed, but only actually
 * fires when the workshop calls {@link #notify}, same as the first.
 */
@Service
@RequiredArgsConstructor
public class ReminderCycleService {

    private static final int DEFAULT_SERVICE_DUE_INTERVAL_DAYS = 90;
    private static final int DEFAULT_SECOND_REMINDER_INTERVAL_DAYS = 7;

    private final ReminderCycleRepository reminderCycleRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final MechanicConfigurationSettingsRepository configurationSettingsRepository;
    private final NotificationService notificationService;

    /**
     * Called whenever a booking transitions to COMPLETED. Closes any existing open cycle for
     * this (mechanic, vehicle, customer) and opens a fresh one — never retrofits the old
     * cycle's dates (§10).
     */
    @Transactional
    public void ensureCycleForCompletedBooking(Booking completedBooking) {
        if (completedBooking.getStatus() != BookingStatus.COMPLETED || completedBooking.getCompletedAt() == null) {
            return;
        }

        User mechanic = completedBooking.getMechanic();
        Vehicle vehicle = completedBooking.getVehicle();
        User customer = completedBooking.getVehicleUser();
        LocalDate completedDate = completedBooking.getCompletedAt().toLocalDate();

        Optional<ReminderCycle> openOpt = reminderCycleRepository.findOpenCycle(mechanic, vehicle, customer);
        if (openOpt.isPresent() && openOpt.get().getLastServiceDate().equals(completedDate)) {
            return; // already tracking this exact completion (e.g. re-invoked on a duplicate call)
        }
        openOpt.ifPresent(c -> {
            c.setStatus(ReminderCycleStatus.CLOSED);
            reminderCycleRepository.save(c);
        });

        int serviceDueIntervalDays = configurationSettingsRepository.findByMechanic(mechanic)
                .map(MechanicConfigurationSettings::getServiceDueIntervalDays)
                .orElse(DEFAULT_SERVICE_DUE_INTERVAL_DAYS);

        reminderCycleRepository.save(ReminderCycle.builder()
                .mechanic(mechanic)
                .vehicle(vehicle)
                .customer(customer)
                .lastServiceDate(completedDate)
                .scheduledServiceDate(completedDate.plusDays(serviceDueIntervalDays))
                .status(ReminderCycleStatus.PLANNED)
                .build());
    }

    @Transactional
    public ReminderNotifyResponse notify(User mechanic, ReminderNotifyRequest request) {
        List<ReminderNotifyItemResult> results = new ArrayList<>();
        int sent = 0, skipped = 0, failed = 0;

        for (String regNo : request.getVehicleRegistrationNumbers()) {
            ReminderNotifyItemResult result = processOne(mechanic, regNo, request.getStage());
            results.add(result);
            if ("SENT".equals(result.result())) {
                sent++;
            } else if ("NOTIFICATION_PROVIDER_FAILED".equals(result.result())) {
                failed++;
            } else {
                skipped++;
            }
        }

        return new ReminderNotifyResponse(sent, skipped, failed, results);
    }

    private ReminderNotifyItemResult processOne(User mechanic, String regNo, ReminderNotifyStage stage) {
        Optional<ReminderCycle> cycleOpt = userVehicleRepository.findByRegistrationNumberIgnoreCase(regNo)
                .flatMap(uv -> reminderCycleRepository.findOpenCycle(mechanic, uv.getVehicle(), uv.getUser()));

        if (cycleOpt.isEmpty()) {
            return new ReminderNotifyItemResult(null, regNo, stage, null, null, null, "REMINDER_CYCLE_NOT_FOUND");
        }
        ReminderCycle cycle = cycleOpt.get();

        if (cycle.getCustomer().getMobileNumber() == null || cycle.getCustomer().getMobileNumber().isBlank()) {
            return itemResult(cycle, regNo, stage, "RIDER_NOT_REACHABLE");
        }

        return stage == ReminderNotifyStage.FIRST_REMINDER
                ? sendFirstReminder(mechanic, cycle, regNo)
                : sendSecondReminder(cycle, regNo);
    }

    private ReminderNotifyItemResult sendFirstReminder(User mechanic, ReminderCycle cycle, String regNo) {
        if (cycle.getFirstReminderSentAt() != null) {
            return itemResult(cycle, regNo, ReminderNotifyStage.FIRST_REMINDER, "ALREADY_SENT");
        }

        LocalDateTime now = LocalDateTime.now();
        int secondIntervalDays = configurationSettingsRepository.findByMechanic(mechanic)
                .map(MechanicConfigurationSettings::getSecondReminderIntervalDays)
                .orElse(DEFAULT_SECOND_REMINDER_INTERVAL_DAYS);

        notificationService.createNotification(
                cycle.getCustomer(), "Service Reminder",
                "Your vehicle service was due on " + cycle.getScheduledServiceDate() + ". Please book a service.",
                NotificationType.GENERAL, "ReminderCycle", cycle.getId());

        cycle.setFirstReminderSentAt(now);
        cycle.setSecondReminderScheduledAt(now.plusDays(secondIntervalDays));
        cycle.setStatus(ReminderCycleStatus.FIRST_SENT);
        reminderCycleRepository.save(cycle);

        return itemResult(cycle, regNo, ReminderNotifyStage.FIRST_REMINDER, "SENT");
    }

    private ReminderNotifyItemResult sendSecondReminder(ReminderCycle cycle, String regNo) {
        if (cycle.getFirstReminderSentAt() == null) {
            return itemResult(cycle, regNo, ReminderNotifyStage.SECOND_REMINDER, "NOT_ELIGIBLE");
        }
        if (cycle.getSecondReminderSentAt() != null) {
            return itemResult(cycle, regNo, ReminderNotifyStage.SECOND_REMINDER, "ALREADY_SENT");
        }
        if (cycle.getSecondReminderScheduledAt() != null
                && cycle.getSecondReminderScheduledAt().isAfter(LocalDateTime.now())) {
            return itemResult(cycle, regNo, ReminderNotifyStage.SECOND_REMINDER, "SECOND_REMINDER_NOT_DUE");
        }

        notificationService.createNotification(
                cycle.getCustomer(), "Second Service Reminder",
                "Reminder: your vehicle service is still due. Please book a service.",
                NotificationType.GENERAL, "ReminderCycle", cycle.getId());

        cycle.setSecondReminderSentAt(LocalDateTime.now());
        cycle.setStatus(ReminderCycleStatus.SECOND_SENT);
        reminderCycleRepository.save(cycle);

        return itemResult(cycle, regNo, ReminderNotifyStage.SECOND_REMINDER, "SENT");
    }

    private ReminderNotifyItemResult itemResult(ReminderCycle cycle, String regNo, ReminderNotifyStage stage, String result) {
        return new ReminderNotifyItemResult(cycle.getId(), regNo, stage, cycle.getFirstReminderSentAt(),
                cycle.getScheduledServiceDate(), cycle.getSecondReminderScheduledAt(), result);
    }
}
