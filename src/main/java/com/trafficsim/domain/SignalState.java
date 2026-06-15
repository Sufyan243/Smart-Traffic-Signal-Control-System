package com.trafficsim.domain;

/**
 * Traffic-signal colour for a single lane. Exactly one lane shows {@link #GREEN} at a time,
 * which corresponds to the lane whose vehicle currently holds the intersection (the CPU).
 */
public enum SignalState {
    RED,
    YELLOW,
    GREEN
}
