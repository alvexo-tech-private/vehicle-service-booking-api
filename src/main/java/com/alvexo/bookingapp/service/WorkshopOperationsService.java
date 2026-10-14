package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.NotificationPreferencesRequest;
import com.alvexo.bookingapp.dto.request.PaymentActionRequest;
import com.alvexo.bookingapp.dto.response.NotificationPreferencesResponse;
import com.alvexo.bookingapp.dto.response.PickupDropStatusResponse;
import com.alvexo.bookingapp.dto.response.WorkshopPaymentResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.BusinessRuleException;
import com.alvexo.bookingapp.model.*;
import com.alvexo.bookingapp.repository.WorkshopNotificationPreferencesRepository;
import com.alvexo.bookingapp.repository.WorkshopPaymentSetupRepository;
import com.alvexo.bookingapp.repository.WorkshopPickupDropStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkshopOperationsService {

    private final WorkshopPickupDropStatusRepository pickupDropRepository;
    private final WorkshopPaymentSetupRepository paymentSetupRepository;
    private final WorkshopNotificationPreferencesRepository notificationPreferencesRepository;

    public WorkshopOperationsService(WorkshopPickupDropStatusRepository pickupDropRepository,
                                      WorkshopPaymentSetupRepository paymentSetupRepository,
                                      WorkshopNotificationPreferencesRepository notificationPreferencesRepository) {
        this.pickupDropRepository = pickupDropRepository;
        this.paymentSetupRepository = paymentSetupRepository;
        this.notificationPreferencesRepository = notificationPreferencesRepository;
    }

    // ── Section 10: Pickup & Drop Status (read-only) ──────────────────────────

    @Transactional
    public PickupDropStatusResponse getPickupDropStatus(User mechanic) {
        validateRole(mechanic);
        WorkshopPickupDropStatus status = pickupDropRepository.findByMechanic(mechanic)
                .orElseGet(() -> pickupDropRepository.save(WorkshopPickupDropStatus.builder().mechanic(mechanic).build()));

        PickupDropStage current = status.getCurrentStage();
        List<PickupDropStatusResponse.StageStatus> statuses = Arrays.stream(PickupDropStage.values())
                .map(stage -> PickupDropStatusResponse.StageStatus.builder()
                        .key(stage)
                        .label(labelFor(stage))
                        .state(stateFor(stage, current))
                        .build())
                .collect(Collectors.toList());

        return PickupDropStatusResponse.builder()
                .facilityEnabled(status.getFacilityEnabled())
                .statuses(statuses)
                .build();
    }

    private PickupDropStageState stateFor(PickupDropStage stage, PickupDropStage current) {
        if (current == null) {
            return PickupDropStageState.PENDING;
        }
        if (stage.ordinal() < current.ordinal()) {
            return PickupDropStageState.DONE;
        }
        return stage == current ? PickupDropStageState.ACTIVE : PickupDropStageState.PENDING;
    }

    private String labelFor(PickupDropStage stage) {
        return switch (stage) {
            case REQUESTED -> "Requested";
            case DRIVER_ASSIGNED -> "Driver Assigned";
            case PICKED_UP -> "Picked Up";
            case IN_SERVICE -> "In Service";
            case RETURNED -> "Returned";
        };
    }

    // ── Section 11: Payment Setup ──────────────────────────────────────────────

    @Transactional
    public WorkshopPaymentResponse getPaymentSetup(User mechanic) {
        validateRole(mechanic);
        WorkshopPaymentSetup setup = getOrCreatePaymentSetup(mechanic);
        return toPaymentResponse(setup);
    }

    private WorkshopPaymentSetup getOrCreatePaymentSetup(User mechanic) {
        return paymentSetupRepository.findByMechanic(mechanic)
                .orElseGet(() -> paymentSetupRepository.save(WorkshopPaymentSetup.builder().mechanic(mechanic).build()));
    }

    @Transactional
    public WorkshopPaymentResponse performPaymentAction(User mechanic, PaymentActionRequest request) {
        validateRole(mechanic);
        WorkshopPaymentSetup setup = getOrCreatePaymentSetup(mechanic);

        switch (request.getAction()) {
            case "request_approval" -> setup.setEligibility(PaymentEligibility.PENDING_APPROVAL);
            case "link_account" -> {
                if (request.getPartner() == null || request.getPartner().isBlank()
                        || request.getAccountRef() == null || request.getAccountRef().isBlank()) {
                    throw new BusinessRuleException("VALIDATION_ERROR",
                            "partner and accountRef are required for link_account");
                }
                setup.setPartner(request.getPartner());
                setup.setAccountRef(request.getAccountRef());
                setup.setEligibility(PaymentEligibility.APPROVED);
            }
            default -> throw new BadRequestException("Unsupported action: " + request.getAction());
        }

        return toPaymentResponse(paymentSetupRepository.save(setup));
    }

    private WorkshopPaymentResponse toPaymentResponse(WorkshopPaymentSetup setup) {
        List<WorkshopPaymentResponse.StepStatus> steps = List.of(
                WorkshopPaymentResponse.StepStatus.builder()
                        .key("eligibility_check").label("Eligibility Check")
                        .state(setup.getEligibility() == PaymentEligibility.NOT_ELIGIBLE ? "pending" : "done").build(),
                WorkshopPaymentResponse.StepStatus.builder()
                        .key("account_link").label("Account Link")
                        .state(setup.getAccountRef() != null ? "done" : "pending").build(),
                WorkshopPaymentResponse.StepStatus.builder()
                        .key("approval").label("Approval")
                        .state(setup.getEligibility() == PaymentEligibility.APPROVED ? "done"
                                : setup.getEligibility() == PaymentEligibility.PENDING_APPROVAL ? "in_progress" : "pending")
                        .build()
        );

        return WorkshopPaymentResponse.builder()
                .eligibility(setup.getEligibility())
                .statuses(steps)
                .partner(setup.getPartner())
                .accountRef(setup.getAccountRef())
                .build();
    }

    // ── Section 12: Notifications ─────────────────────────────────────────────

    @Transactional
    public NotificationPreferencesResponse getNotificationPreferences(User mechanic) {
        validateRole(mechanic);
        WorkshopNotificationPreferences prefs = getOrCreateNotificationPreferences(mechanic);
        return toNotificationResponse(prefs);
    }

    private WorkshopNotificationPreferences getOrCreateNotificationPreferences(User mechanic) {
        return notificationPreferencesRepository.findByMechanic(mechanic)
                .orElseGet(() -> notificationPreferencesRepository.save(
                        WorkshopNotificationPreferences.builder().mechanic(mechanic).build()));
    }

    @Transactional
    public NotificationPreferencesResponse saveNotificationPreferences(User mechanic, NotificationPreferencesRequest request) {
        validateRole(mechanic);
        if (request.getChannels().isEmpty()) {
            throw new BusinessRuleException("CHANNELS_REQUIRED", "At least one notification channel is required");
        }

        WorkshopNotificationPreferences prefs = getOrCreateNotificationPreferences(mechanic);
        prefs.setNewBookingEnabled(request.getNewBookingEnabled());
        prefs.setBookingCancelledEnabled(request.getBookingCancelledEnabled());
        prefs.setBookingRescheduledEnabled(request.getBookingRescheduledEnabled());
        prefs.setChannels(request.getChannels());
        prefs.setSound(request.getSound());

        return toNotificationResponse(notificationPreferencesRepository.save(prefs));
    }

    private NotificationPreferencesResponse toNotificationResponse(WorkshopNotificationPreferences prefs) {
        return NotificationPreferencesResponse.builder()
                .booking(NotificationPreferencesResponse.BookingToggles.builder()
                        .newBooking(prefs.getNewBookingEnabled())
                        .bookingCancelled(prefs.getBookingCancelledEnabled())
                        .bookingRescheduled(prefs.getBookingRescheduledEnabled())
                        .build())
                .payment(NotificationPreferencesResponse.PaymentToggle.builder()
                        .enabled(true).locked(true).build())
                .channels(prefs.getChannels())
                .sound(prefs.getSound())
                .build();
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only workshops (mechanics) can manage workshop operations");
        }
    }
}
