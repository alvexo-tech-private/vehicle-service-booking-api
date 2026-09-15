package com.alvexo.bookingapp.util;

import com.alvexo.bookingapp.model.Booking;
import com.alvexo.bookingapp.model.BookingType;
import com.alvexo.bookingapp.model.CapacityBand;
import com.alvexo.bookingapp.model.JobCardNumberFormat;
import com.alvexo.bookingapp.model.MechanicConfigurationSettings;
import com.alvexo.bookingapp.model.ServiceDeskStage;
import com.alvexo.bookingapp.model.ServiceType;
import com.alvexo.bookingapp.model.ServiceWorkspaceStatus;
import com.alvexo.bookingapp.model.VehicleType;
import com.alvexo.bookingapp.model.WeekStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pure mapping/derivation helpers shared by the Service Desk services
 * (SERVICE_DESK_API_SPEC.md). Kept static and side-effect free so every tab
 * derives status/label/capacity the same way.
 */
public final class ServiceDeskMapper {

    private static final Pattern NUMERIC_TAIL = Pattern.compile("(\\d+)$");
    private static final DateTimeFormatter REPORTING_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    /** Booking.serviceType values treated as "Repairs" for the Service Desk abbreviation (§0 shared enums). */
    private static final Set<ServiceType> REPAIR_SERVICE_TYPES = EnumSet.of(
            ServiceType.GENERAL_REPAIR, ServiceType.ENGINE_DIAGNOSTIC,
            ServiceType.TRANSMISSION_SERVICE, ServiceType.ELECTRICAL_REPAIR, ServiceType.BODYWORK);

    private ServiceDeskMapper() {
    }

    /**
     * Booking status → Service Desk "Today" status (§0.1 status mapping table).
     * Arrived vs Pending is resolved from the persisted serviceStage sub-state.
     */
    public static ServiceWorkspaceStatus toWorkspaceStatus(Booking booking) {
        return switch (booking.getStatus()) {
            case CONFIRMED -> ServiceWorkspaceStatus.SCHEDULED;
            case IN_PROGRESS -> booking.getServiceStage() == ServiceDeskStage.PENDING
                    ? ServiceWorkspaceStatus.PENDING : ServiceWorkspaceStatus.ARRIVED;
            case COMPLETED -> ServiceWorkspaceStatus.COMPLETED;
            case CANCELLED, REJECTED -> ServiceWorkspaceStatus.CANCELLED;
            case PENDING -> throw new IllegalStateException(
                    "PENDING bookings must be excluded from the Service Desk before mapping");
        };
    }

    /** Booking status → Service Desk "Service Week" status (only SCHEDULED/CANCELLED exist there). */
    public static WeekStatus toWeekStatus(Booking booking) {
        return booking.getStatus().name().equals("CANCELLED") ? WeekStatus.CANCELLED : WeekStatus.SCHEDULED;
    }

    /**
     * Best-effort ServiceTypeAbbr (§0 shared enums, ≤8 chars). The underlying
     * VehicleType enum has no Bike/Scooter distinction (only MOTORCYCLE covers
     * two-wheelers), so MOTORCYCLE is labelled "Bike" and every other vehicle
     * type falls back to a generic "Veh" family; only the General/Express
     * split (from BookingType) and the Repairs bucket are fully accurate.
     */
    public static String serviceTypeAbbr(Booking booking) {
        if (REPAIR_SERVICE_TYPES.contains(booking.getServiceType())) {
            return "Repairs";
        }
        String family = booking.getVehicle() != null && booking.getVehicle().getVehicleType() == VehicleType.MOTORCYCLE
                ? "Bike" : "Veh";
        String suffix = booking.getBookingType() == BookingType.EXPRESS ? "Exp" : "Gen";
        return family + " " + suffix;
    }

    /** Last 4 characters of a registration number, ignoring spaces (e.g. "TN 09 AB 4587" → "4587"). */
    public static String regnLast4(String registrationNumber) {
        if (registrationNumber == null) {
            return null;
        }
        String compact = registrationNumber.replace(" ", "");
        return compact.length() <= 4 ? compact : compact.substring(compact.length() - 4);
    }

    public static String formatReportingTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(REPORTING_TIME_FORMAT);
    }

    public static String fullName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    /** Per-date capacity band (§2.2): full ≥1.0, high ≥0.75, mod ≥0.5, else low. */
    public static CapacityBand capacityBand(long booked, Integer capacity) {
        if (capacity == null || capacity <= 0) {
            return booked > 0 ? CapacityBand.FULL : CapacityBand.LOW;
        }
        BigDecimal ratio = BigDecimal.valueOf(booked)
                .divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) >= 0) {
            return CapacityBand.FULL;
        }
        if (ratio.compareTo(new BigDecimal("0.75")) >= 0) {
            return CapacityBand.HIGH;
        }
        if (ratio.compareTo(new BigDecimal("0.5")) >= 0) {
            return CapacityBand.MOD;
        }
        return CapacityBand.LOW;
    }

    /**
     * ₹30 Service Reliability Adjustment (§1.7, BR-22/30): applies when the
     * cancellation happens after the mechanic's configured reschedule cutoff
     * time-of-day. Null cutoff means the rule never applies.
     */
    public static BigDecimal computeReliabilityAdjustment(LocalTime rescheduleCutoffTime, LocalTime now) {
        return rescheduleCutoffTime != null && now.isAfter(rescheduleCutoffTime)
                ? Constants.SERVICE_RELIABILITY_ADJUSTMENT : null;
    }

    /**
     * Computes the next job card number from the mechanic's job-card config
     * and their already-issued numbers (§1.6):
     *  - strip prefix/suffix, parse the trailing numeric run
     *  - next = max(existing tails) + 1, or the starting sequence's own tail if none exist
     *  - NUMERIC: {prefix}{padded number}{suffix}
     *  - ALPHANUMERIC: preserve the starting sequence's leading letters (e.g. "A001" → "A002")
     */
    public static String computeNextJobCardNumber(MechanicConfigurationSettings config,
                                                    List<String> existingJobCardNumbers) {
        String prefix = config.getJobCardNumberPrefix() != null ? config.getJobCardNumberPrefix() : "";
        String suffix = config.getJobCardNumberSuffix() != null ? config.getJobCardNumberSuffix() : "";
        String startingSequence = config.getJobCardNumberStartingSequence() != null
                && !config.getJobCardNumberStartingSequence().isBlank()
                ? config.getJobCardNumberStartingSequence() : "0001";
        JobCardNumberFormat format = config.getJobCardNumberFormat() != null
                ? config.getJobCardNumberFormat() : JobCardNumberFormat.NUMERIC;

        String alphaPrefix = format == JobCardNumberFormat.ALPHANUMERIC ? leadingLetters(startingSequence) : "";

        List<Long> existingTails = existingJobCardNumbers.stream()
                .map(jc -> extractNumericTail(stripPrefixSuffix(jc, prefix, suffix)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Long startingTail = extractNumericTail(startingSequence.substring(alphaPrefix.length()));
        long startingNum = startingTail != null ? startingTail : 1L;

        long nextNum = existingTails.isEmpty()
                ? startingNum
                : existingTails.stream().mapToLong(Long::longValue).max().getAsLong() + 1;

        int width = Math.max(4, startingSequence.length() - alphaPrefix.length());
        return prefix + alphaPrefix + zeroPad(nextNum, width) + suffix;
    }

    private static String stripPrefixSuffix(String value, String prefix, String suffix) {
        String result = value;
        if (!prefix.isEmpty() && result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        if (!suffix.isEmpty() && result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private static Long extractNumericTail(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = NUMERIC_TAIL.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private static String leadingLetters(String value) {
        int i = 0;
        while (i < value.length() && Character.isLetter(value.charAt(i))) {
            i++;
        }
        return value.substring(0, i);
    }

    private static String zeroPad(long num, int width) {
        return String.format("%0" + width + "d", num);
    }
}
