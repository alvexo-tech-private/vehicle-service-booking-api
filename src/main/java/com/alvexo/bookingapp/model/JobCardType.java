// JobCardType.java
package com.alvexo.bookingapp.model;

public enum JobCardType {
    AUTO_JOB_CARD,          // Type 1 — vehicle count, no capacity reservation
                            // Type 2 — vehicle count + hour-based capacity reservation
    MECHANIC_JOB_CARD       // Type 3 — AA + RC hour split, no repair slots
                            // Type 4 — AA + RC hour split + dedicated repair slots
}