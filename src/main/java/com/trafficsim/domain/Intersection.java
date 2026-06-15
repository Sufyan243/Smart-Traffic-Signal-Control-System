package com.trafficsim.domain;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The intersection modelled as the CPU / a single shared resource.
 *
 * <p>Only one vehicle may cross at any instant, so this class is the project's
 * <em>critical-section / mutual-exclusion</em> demonstration. The green light is a lock held by
 * exactly one lane: a vehicle must {@link #acquire(Vehicle) acquire} the intersection before it can
 * start crossing and {@link #release() release} it when finished. A {@link ReentrantLock} backs the
 * grant so the synchronization primitive is real, even though the simulation loop itself is
 * single-threaded and deterministic.</p>
 */
public final class Intersection {

    private final ReentrantLock lock = new ReentrantLock();
    private Vehicle occupant;       // the vehicle currently crossing, or null
    private int greenLaneId = -1;   // lane that currently holds the green light, or -1

    /** Grants the intersection to {@code vehicle}. Returns false if it is already held by another. */
    public boolean acquire(Vehicle vehicle) {
        if (occupant != null && occupant != vehicle) {
            return false;
        }
        lock.lock();
        try {
            occupant = vehicle;
            greenLaneId = vehicle.laneId();
            return true;
        } finally {
            // Lock is released immediately; it exists to make the mutual-exclusion guarantee explicit
            // and unit-testable rather than to coordinate OS threads in this single-threaded engine.
            lock.unlock();
        }
    }

    /** Frees the intersection (e.g. crossing complete or vehicle preempted). */
    public void release() {
        occupant = null;
        greenLaneId = -1;
    }

    public Vehicle occupant() {
        return occupant;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public int greenLaneId() {
        return greenLaneId;
    }

    public void reset() {
        release();
    }
}
