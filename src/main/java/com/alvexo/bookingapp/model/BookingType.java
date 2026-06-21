package com.alvexo.bookingapp.model;

public enum BookingType {
    /** Vehicle reports on or after expressReportingTime → standard queue */
    STANDARD,

    /** Vehicle is scheduled in single fixed time — No slot choice */
    GENERAL,

    /** Vehicle reports before expressReportingTime → priority / express queue */
    EXPRESS,

    /** Vehicle is scheduled in General time OR either of the two repair-specific slots */
    REPAIRS,

    /** Vehicle is scheduled in single fixed time, just like General — No slot choice */
    COMPLEX
}
