package com.trafficsim.metrics;

/**
 * Per-vehicle scheduling outcome for one run, derived from the vehicle's arrival / first-start /
 * completion timestamps.
 *
 * <pre>
 *   turnaround = completion - arrival
 *   waiting    = turnaround - burst        (time spent not crossing)
 *   response   = firstStart - arrival      (delay until first granted the intersection)
 * </pre>
 */
public record VehicleMetrics(
        int vehicleId,
        int laneId,
        int arrivalTime,
        int burstTime,
        int firstStartTime,
        int completionTime,
        int waitingTime,
        int turnaroundTime,
        int responseTime,
        boolean emergency) {
}
