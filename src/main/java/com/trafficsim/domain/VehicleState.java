package com.trafficsim.domain;

/**
 * Lifecycle of a vehicle-process inside one simulation run.
 *
 * <pre>
 *   WAITING  -> queued at its lane, has arrived but not yet been granted the intersection
 *   CROSSING -> currently holds the intersection (the "green" / running process)
 *   DONE     -> finished crossing (process terminated)
 * </pre>
 */
public enum VehicleState {
    WAITING,
    CROSSING,
    DONE
}
