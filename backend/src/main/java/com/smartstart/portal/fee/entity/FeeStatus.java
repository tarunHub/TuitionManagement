package com.smartstart.portal.fee.entity;

/** Derived from amount due vs. amount paid; never stored. */
public enum FeeStatus {
    UNPAID,
    PARTIAL,
    PAID,
    CREDIT
}
