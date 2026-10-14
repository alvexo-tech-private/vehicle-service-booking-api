package com.alvexo.bookingapp.service;

import com.alvexo.bookingapp.model.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers BookingService's general-booking-vs-slot-booking reporting-time validation:
 *  - Level 1 general bookings (GENERAL_SERVICE / EXPRESS_SERVICE): the configured reporting
 *    time is informational only — only the booking DATE can't be in the past.
 *  - Advanced Level slot bookings (GENERAL_REPAIR): the complete slot date+time must be
 *    strictly later than now; equal-to-now is already too late.
 */
class BookingServiceTest {

    private static final LocalDateTime NOON_50 = LocalDateTime.of(2026, 10, 10, 12, 50);
    private static final LocalDateTime ONE_PM = LocalDateTime.of(2026, 10, 10, 13, 0);
    private static final LocalDateTime ONE_TEN_PM = LocalDateTime.of(2026, 10, 10, 13, 10);
    private static final LocalDateTime TEN_AM = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Nested
    @DisplayName("isSlotBooking")
    class IsSlotBooking {

        @Test
        @DisplayName("GENERAL_REPAIR is the only slot booking type")
        void onlyGeneralRepairIsSlotBooking() {
            assertThat(BookingService.isSlotBooking(ServiceType.GENERAL_REPAIR)).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = ServiceType.class, names = "GENERAL_REPAIR", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("every other service type is a general booking")
        void everythingElseIsGeneral(ServiceType serviceType) {
            assertThat(BookingService.isSlotBooking(serviceType)).isFalse();
        }
    }

    @Nested
    @DisplayName("Level 1 general bookings (GENERAL_SERVICE / EXPRESS_SERVICE) — reporting time is informational")
    class GeneralBookingTimeValidation {

        @Test
        @DisplayName("same-day booking whose configured reporting time has already passed is still accepted")
        void sameDayPastReportingTimeIsAccepted() {
            LocalDateTime scheduled = LocalDateTime.of(2026, 10, 10, 8, 30);
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 20, 30);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_SERVICE, scheduled, now)).isFalse();
            assertThat(BookingService.isPastBooking(ServiceType.EXPRESS_SERVICE, scheduled, now)).isFalse();
        }

        @Test
        @DisplayName("a booking dated before today is still rejected")
        void pastDateIsRejected() {
            LocalDateTime scheduled = LocalDateTime.of(2026, 10, 9, 23, 59);
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 0, 1);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_SERVICE, scheduled, now)).isTrue();
        }

        @Test
        @DisplayName("today's date is accepted at any time of day, regardless of the clock")
        void todayAtAnyTimeIsAccepted() {
            LocalDateTime scheduled = LocalDateTime.of(2026, 10, 10, 0, 0);
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 23, 59);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_SERVICE, scheduled, now)).isFalse();
        }

        @Test
        @DisplayName("a future date is always accepted")
        void futureDateIsAccepted() {
            LocalDateTime scheduled = LocalDateTime.of(2026, 10, 11, 8, 30);
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 20, 30);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_SERVICE, scheduled, now)).isFalse();
        }
    }

    @Nested
    @DisplayName("Advanced Level slot bookings (GENERAL_REPAIR) — strict future-time check")
    class SlotBookingTimeValidation {

        @Test
        @DisplayName("12:50 PM booking for today's 1:00 PM slot is accepted")
        void beforeSlotTimeIsAccepted() {
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, ONE_PM, NOON_50)).isFalse();
        }

        @Test
        @DisplayName("1:00 PM booking for today's 1:00 PM slot is rejected (equal time is already too late)")
        void exactSlotTimeIsRejected() {
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, ONE_PM, ONE_PM)).isTrue();
        }

        @Test
        @DisplayName("1:10 PM booking for today's 1:00 PM slot is rejected")
        void afterSlotTimeIsRejected() {
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, ONE_PM, ONE_TEN_PM)).isTrue();
        }

        @Test
        @DisplayName("10:00 AM booking for today's 8:30 AM slot is rejected")
        void todayEarlierSlotIsRejected() {
            LocalDateTime eightThirty = LocalDateTime.of(2026, 10, 10, 8, 30);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, eightThirty, TEN_AM)).isTrue();
        }

        @Test
        @DisplayName("10:00 AM booking for tomorrow's 8:30 AM slot is accepted")
        void tomorrowSlotIsAccepted() {
            LocalDateTime tomorrowEightThirty = LocalDateTime.of(2026, 10, 11, 8, 30);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, tomorrowEightThirty, TEN_AM)).isFalse();
        }

        @Test
        @DisplayName("day boundary: a slot at 12:00 AM the next day is accepted when now is 11:59 PM today")
        void dayBoundaryNextDayMidnightSlotIsAccepted() {
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 23, 59);
            LocalDateTime midnightSlot = LocalDateTime.of(2026, 10, 11, 0, 0);
            assertThat(BookingService.isPastBooking(ServiceType.GENERAL_REPAIR, midnightSlot, now)).isFalse();
        }
    }
}
