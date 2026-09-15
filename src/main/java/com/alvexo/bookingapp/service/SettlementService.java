package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.dto.request.SettlementQueryRequest;
import com.alvexo.bookingapp.dto.request.SettlementStatusUpdateRequest;
import com.alvexo.bookingapp.dto.response.SettlementFaqEntryResponse;
import com.alvexo.bookingapp.dto.response.SettlementQueryResponse;
import com.alvexo.bookingapp.dto.response.SettlementResponse;
import com.alvexo.bookingapp.exception.BadRequestException;
import com.alvexo.bookingapp.exception.ResourceNotFoundException;
import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.Settlement;
import com.alvexo.bookingapp.model.SettlementQuery;
import com.alvexo.bookingapp.model.SettlementStatus;
import com.alvexo.bookingapp.model.User;
import com.alvexo.bookingapp.model.UserRole;
import com.alvexo.bookingapp.repository.BookingRepository;
import com.alvexo.bookingapp.repository.SettlementQueryRepository;
import com.alvexo.bookingapp.repository.SettlementRepository;
import com.alvexo.bookingapp.util.Constants;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Backs the Workshop Finance & Settlement tab (WORKSHOP_FINANCE_API_SPEC.md).
 *
 * Settlements have no separate write path from the app side — a row is
 * materialized lazily the first time it's asked for, from that day's booking
 * data (advancePaid + reliabilityAdjustmentAmount), for any date strictly
 * before today. "Today" is never settled here; that's the live Earnings tab's
 * job. Once created, a settlement's netPay/adjustment are frozen — later
 * edits to a booking don't retroactively change a payout that's already been
 * computed, same as a real settlement wouldn't be silently rewritten.
 */
@Service
public class SettlementService {

    private static final DateTimeFormatter QUERY_REF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SettlementRepository settlementRepository;
    private final SettlementQueryRepository settlementQueryRepository;
    private final BookingRepository bookingRepository;

    public SettlementService(SettlementRepository settlementRepository,
                              SettlementQueryRepository settlementQueryRepository,
                              BookingRepository bookingRepository) {
        this.settlementRepository = settlementRepository;
        this.settlementQueryRepository = settlementQueryRepository;
        this.bookingRepository = bookingRepository;
    }

    // ── §2: Settlement list ─────────────────────────────────────────────────

    @Transactional
    public List<SettlementResponse> getSettlements(User mechanic, Integer year, Integer month,
                                                     LocalDate from, LocalDate to, Integer limit) {
        validateRole(mechanic);
        LocalDate today = LocalDate.now();

        if (year != null || month != null) {
            if (year == null || month == null) {
                throw new BadRequestException("year and month are both required for the month filter");
            }
            YearMonth requested = toYearMonth(year, month);
            validateMonthFilter(requested, today);
            ensureGenerated(mechanic, requested.atDay(1), requested.atEndOfMonth(), today);
            return settlementRepository
                    .findByMechanicAndSettlementDateBetweenOrderBySettlementDateDesc(
                            mechanic, requested.atDay(1), requested.atEndOfMonth())
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        if (from != null || to != null) {
            if (from == null || to == null) {
                throw new BadRequestException("from and to are both required for a custom range");
            }
            validateCustomRange(from, to, today);
            ensureGenerated(mechanic, from, to, today);
            return settlementRepository
                    .findByMechanicAndSettlementDateBetweenOrderBySettlementDateDesc(mechanic, from, to)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        int effectiveLimit = limit != null && limit > 0 ? Math.min(limit, 100) : 7;
        LocalDate lookbackStart = today.minusDays(Constants.SETTLEMENT_LATEST_LOOKBACK_DAYS);
        ensureGenerated(mechanic, lookbackStart, today.minusDays(1), today);
        return settlementRepository.findByMechanicOrderBySettlementDateDesc(mechanic).stream()
                .limit(effectiveLimit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlementById(User mechanic, Long id) {
        validateRole(mechanic);
        Settlement settlement = settlementRepository.findByIdAndMechanic(id, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        return toResponse(settlement);
    }

    // ── §4: PDFs ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Settlement getPaidSettlementForReceipt(User mechanic, Long id) {
        validateRole(mechanic);
        Settlement settlement = settlementRepository.findByIdAndMechanic(id, mechanic)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        if (settlement.getStatus() != SettlementStatus.PAID) {
            throw new BadRequestException("A receipt is only available for a Paid settlement");
        }
        return settlement;
    }

    @Transactional
    public List<Settlement> getStatementSettlements(User mechanic, Integer year, Integer month,
                                                      LocalDate from, LocalDate to) {
        validateRole(mechanic);
        LocalDate today = LocalDate.now();
        LocalDate periodFrom;
        LocalDate periodTo;

        if (year != null || month != null) {
            if (year == null || month == null) {
                throw new BadRequestException("year and month are both required for the month filter");
            }
            YearMonth requested = toYearMonth(year, month);
            validateMonthFilter(requested, today);
            periodFrom = requested.atDay(1);
            periodTo = requested.atEndOfMonth();
        } else if (from != null || to != null) {
            if (from == null || to == null) {
                throw new BadRequestException("from and to are both required for a custom range");
            }
            validateCustomRange(from, to, today);
            periodFrom = from;
            periodTo = to;
        } else {
            periodTo = today.minusDays(1);
            periodFrom = today.minusDays(Constants.SETTLEMENT_LATEST_LOOKBACK_DAYS);
        }

        ensureGenerated(mechanic, periodFrom, periodTo, today);
        return settlementRepository
                .findByMechanicAndSettlementDateBetweenOrderBySettlementDateDesc(mechanic, periodFrom, periodTo);
    }

    // ── §5: FAQ (static — low priority per spec, revisit if copy needs to change without a release) ──

    public List<SettlementFaqEntryResponse> getFaq() {
        String cap = Constants.SETTLEMENT_DAILY_ADJUSTMENT_CAP.stripTrailingZeros().toPlainString();
        String perOccurrence = Constants.SERVICE_RELIABILITY_ADJUSTMENT.stripTrailingZeros().toPlainString();
        String suspension = Constants.SETTLEMENT_SUSPENSION_THRESHOLD.stripTrailingZeros().toPlainString();
        return List.of(
                SettlementFaqEntryResponse.builder()
                        .question("Why was money deducted from my settlement?")
                        .answer("A ₹" + perOccurrence + " Service Reliability Adjustment applies to each booking "
                                + "cancelled, marked no-show, or flagged for a quality complaint after the "
                                + "reschedule cutoff time, capped at ₹" + cap + " per day.")
                        .build(),
                SettlementFaqEntryResponse.builder()
                        .question("How is my settlement released amount calculated?")
                        .answer("settlementReleased = max(0, netPay - serviceReliabilityAdjustment) for that "
                                + "service day.")
                        .build(),
                SettlementFaqEntryResponse.builder()
                        .question("When do I get paid?")
                        .answer("Settlements are released within 2 working days of the service day completing.")
                        .build(),
                SettlementFaqEntryResponse.builder()
                        .question("What happens if my adjustments add up?")
                        .answer("Accumulating ₹" + suspension + " in adjustments suspends new bookings pending "
                                + "review.")
                        .build()
        );
    }

    // ── §6: Raise Settlement Query ───────────────────────────────────────────

    @Transactional
    public SettlementQueryResponse submitQuery(User mechanic, SettlementQueryRequest request) {
        validateRole(mechanic);
        Settlement settlement = settlementRepository
                .findByMechanicAndSettlementDate(mechanic, request.getSettlementDate())
                .orElseThrow(() -> new BadRequestException(
                        "No settlement found for " + request.getSettlementDate()));

        LocalDateTime now = LocalDateTime.now();
        SettlementQuery query = SettlementQuery.builder()
                .mechanic(mechanic)
                .settlement(settlement)
                .referenceId(generateQueryReference(now.toLocalDate()))
                .description(request.getDescription())
                .build();
        query = settlementQueryRepository.save(query);

        return SettlementQueryResponse.builder()
                .queryReference(query.getReferenceId())
                .queryStatus(query.getStatus())
                .submissionDate(now.toLocalDate())
                .submissionTime(now.toLocalTime().withNano(0))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SettlementQueryResponse> getQueries(User mechanic) {
        validateRole(mechanic);
        return settlementQueryRepository.findByMechanicOrderByCreatedAtDesc(mechanic).stream()
                .map(q -> SettlementQueryResponse.builder()
                        .queryReference(q.getReferenceId())
                        .queryStatus(q.getStatus())
                        .submissionDate(q.getCreatedAt().toLocalDate())
                        .submissionTime(q.getCreatedAt().toLocalTime().withNano(0))
                        .settlementDate(q.getSettlement().getSettlementDate())
                        .description(q.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Admin: settlement status transitions (not part of the mobile spec — ──
    // ── something has to be able to mark a settlement Paid once money moves) ──

    @Transactional
    public SettlementResponse updateStatus(Long id, SettlementStatusUpdateRequest request) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));

        if (request.getStatus() == SettlementStatus.PAID) {
            if (isBlank(request.getTransactionNumber()) || isBlank(request.getPaymentGateway())
                    || isBlank(request.getPaymentGatewayReference()) || request.getPaymentDate() == null
                    || request.getPaymentTime() == null || isBlank(request.getPaymentStatus())) {
                throw new BadRequestException(
                        "transactionNumber, paymentGateway, paymentGatewayReference, paymentDate, "
                                + "paymentTime and paymentStatus are all required when marking a settlement Paid");
            }
            settlement.setTransactionNumber(request.getTransactionNumber());
            settlement.setPaymentGateway(request.getPaymentGateway());
            settlement.setPaymentGatewayReference(request.getPaymentGatewayReference());
            settlement.setPaymentDate(request.getPaymentDate());
            settlement.setPaymentTime(request.getPaymentTime());
            settlement.setPaymentStatus(request.getPaymentStatus());
        }
        settlement.setStatus(request.getStatus());
        return toResponse(settlementRepository.save(settlement));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Materializes a Settlement row for every past date in [from, min(to, yesterday)] that has booking activity. */
    private void ensureGenerated(User mechanic, LocalDate from, LocalDate to, LocalDate today) {
        LocalDate effectiveTo = to.isBefore(today) ? to : today.minusDays(1);
        if (effectiveTo.isBefore(from)) {
            return;
        }
        for (LocalDate date = from; !date.isAfter(effectiveTo); date = date.plusDays(1)) {
            generateIfMissing(mechanic, date);
        }
    }

    private void generateIfMissing(User mechanic, LocalDate date) {
        if (settlementRepository.findByMechanicAndSettlementDate(mechanic, date).isPresent()) {
            return;
        }
        List<Booking> bookings = bookingRepository.findEarningsBookings(mechanic, date);
        if (bookings.isEmpty()) {
            return;
        }

        BigDecimal netPay = bookings.stream()
                .map(Booking::getAdvancePaid)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rawAdjustment = bookings.stream()
                .map(Booking::getReliabilityAdjustmentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adjustment = rawAdjustment.min(Constants.SETTLEMENT_DAILY_ADJUSTMENT_CAP);

        BigDecimal released = netPay.subtract(adjustment).max(BigDecimal.ZERO);

        Settlement settlement = Settlement.builder()
                .mechanic(mechanic)
                .settlementDate(date)
                .netPay(netPay)
                .serviceReliabilityAdjustment(adjustment)
                .settlementReleased(released)
                .status(SettlementStatus.PENDING)
                .build();
        try {
            settlementRepository.save(settlement);
        } catch (DataIntegrityViolationException e) {
            // Concurrent generation for the same (mechanic, date) raced us — the row exists now, nothing to do.
        }
    }

    private YearMonth toYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        return YearMonth.of(year, month);
    }

    private void validateMonthFilter(YearMonth requested, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth latestAllowed = currentMonth.minusMonths(1);
        YearMonth earliestAllowed = currentMonth.minusMonths(3);
        if (requested.isAfter(latestAllowed) || requested.isBefore(earliestAllowed)) {
            throw new BadRequestException(
                    "month must be one of the last 3 completed months (" + earliestAllowed + " to " + latestAllowed + ")");
        }
    }

    private void validateCustomRange(LocalDate from, LocalDate to, LocalDate today) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must not be after to");
        }
        if (!to.isBefore(today)) {
            throw new BadRequestException("to must be a past date");
        }
        if (from.isBefore(today.minusDays(Constants.SETTLEMENT_CUSTOM_RANGE_LOOKBACK_DAYS))) {
            throw new BadRequestException(
                    "from must be within the previous " + Constants.SETTLEMENT_CUSTOM_RANGE_LOOKBACK_DAYS + " days");
        }
        long totalDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (totalDays > Constants.SETTLEMENT_CUSTOM_RANGE_MAX_DAYS) {
            throw new BadRequestException(
                    "custom range must not exceed " + Constants.SETTLEMENT_CUSTOM_RANGE_MAX_DAYS + " days");
        }
    }

    private String generateQueryReference(LocalDate date) {
        String prefix = "SQ-" + date.format(QUERY_REF_DATE_FORMAT) + "-";
        long seq = settlementQueryRepository.countByReferenceIdStartingWith(prefix) + 1;
        return prefix + String.format("%05d", seq);
    }

    private void validateRole(User mechanic) {
        if (mechanic.getRole() != UserRole.MECHANIC) {
            throw new BadRequestException("Only workshops (mechanics) can view settlements");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SettlementResponse toResponse(Settlement s) {
        boolean paid = s.getStatus() == SettlementStatus.PAID;
        return SettlementResponse.builder()
                .id(s.getId())
                .settlementDate(s.getSettlementDate())
                .netPay(s.getNetPay())
                .serviceReliabilityAdjustment(s.getServiceReliabilityAdjustment())
                .settlementReleased(s.getSettlementReleased())
                .status(s.getStatus())
                .transactionNumber(paid ? s.getTransactionNumber() : null)
                .paymentGateway(paid ? s.getPaymentGateway() : null)
                .paymentGatewayReference(paid ? s.getPaymentGatewayReference() : null)
                .paymentDate(paid ? s.getPaymentDate() : null)
                .paymentTime(paid ? s.getPaymentTime() : null)
                .paymentStatus(paid ? s.getPaymentStatus() : null)
                .workshopName(s.getMechanic().getWorkshopName())
                .workshopId("WS-" + s.getMechanic().getId())
                .build();
    }
}
