package com.trafficsim.metrics;

import com.trafficsim.domain.Vehicle;
import com.trafficsim.domain.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Records the Gantt timeline of a single run as the engine ticks. Each tick reports which vehicle
 * (if any) held the intersection; the collector coalesces consecutive ticks for the same vehicle
 * into {@link GanttSegment}s. Per-vehicle timing is read straight off the {@link Vehicle} timestamps
 * by {@link MetricsCalculator}, so this collector only needs to capture the timeline.
 */
public final class MetricsCollector {

    private final List<GanttSegment> segments = new ArrayList<>();

    private int currentVehicleId = -1;
    private int currentLaneId = -1;
    private VehicleType currentType;
    private int segmentStart = 0;
    private int makespan = 0;

    /** Reports the vehicle holding the intersection during {@code tick} (null = idle tick). */
    public void onTick(int tick, Vehicle running) {
        int id = running == null ? -1 : running.id();
        if (id != currentVehicleId) {
            closeSegment(tick);
            currentVehicleId = id;
            if (running != null) {
                currentLaneId = running.laneId();
                currentType = running.type();
                segmentStart = tick;
            }
        }
    }

    /** Closes the final open segment; {@code finalTick} is the makespan (total ticks elapsed). */
    public void finish(int finalTick) {
        closeSegment(finalTick);
        this.makespan = finalTick;
    }

    public List<GanttSegment> segments() {
        return List.copyOf(segments);
    }

    public int makespan() {
        return makespan;
    }

    public void reset() {
        segments.clear();
        currentVehicleId = -1;
        currentLaneId = -1;
        currentType = null;
        segmentStart = 0;
        makespan = 0;
    }

    private void closeSegment(int endTick) {
        if (currentVehicleId != -1 && endTick > segmentStart) {
            segments.add(new GanttSegment(currentVehicleId, currentLaneId, currentType, segmentStart, endTick));
        }
        currentVehicleId = -1;
    }
}
