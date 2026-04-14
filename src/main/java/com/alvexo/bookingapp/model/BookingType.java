package com.alvexo.bookingapp.model;

public enum BookingType {
    /** Vehicle reports on or after expressReportingTime → standard queue */
    STANDARD,
    /** Vehicle reports before expressReportingTime → priority / express queue */
    EXPRESS
}
