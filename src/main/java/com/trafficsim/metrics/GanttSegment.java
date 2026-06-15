package com.trafficsim.metrics;

import com.trafficsim.domain.VehicleType;

/**
 * One contiguous block in a run's Gantt chart: vehicle {@code vehicleId} held the intersection for
 * the half-open tick interval {@code [startTick, endTick)}. Round Robin and priority preemption can
 * produce several segments for the same vehicle.
 */
public record GanttSegment(int vehicleId, int laneId, VehicleType type, int startTick, int endTick) {

    public int duration() {
        return endTick - startTick;
    }
}
