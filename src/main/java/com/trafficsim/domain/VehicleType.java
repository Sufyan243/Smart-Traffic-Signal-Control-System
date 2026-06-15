package com.trafficsim.domain;

import javafx.scene.paint.Color;

/**
 * Vehicle classes available in the simulator.
 *
 * <p>In CPU-scheduling terms each type fixes a default <em>burst time</em> (how many ticks the
 * vehicle occupies the intersection) and a default <em>priority</em> (lower number = higher
 * priority, the classic convention). {@link #EMERGENCY} is the only type flagged as an emergency
 * and always carries the highest priority.</p>
 */
public enum VehicleType {
    CAR("Car", 2, 5, false, Color.web("#4a9eff")),
    BUS("Bus", 4, 6, false, Color.web("#f59e0b")),
    TRUCK("Truck", 5, 7, false, Color.web("#a78bfa")),
    EMERGENCY("Emergency", 2, 0, true, Color.web("#ef4444"));

    private final String label;
    private final int defaultBurstTime;
    private final int defaultPriority;
    private final boolean emergency;
    private final Color color;

    VehicleType(String label, int defaultBurstTime, int defaultPriority, boolean emergency, Color color) {
        this.label = label;
        this.defaultBurstTime = defaultBurstTime;
        this.defaultPriority = defaultPriority;
        this.emergency = emergency;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public int defaultBurstTime() {
        return defaultBurstTime;
    }

    public int defaultPriority() {
        return defaultPriority;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public Color color() {
        return color;
    }
}
