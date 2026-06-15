package com.trafficsim.metrics;

import com.trafficsim.domain.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the four headline scheduling metrics — average waiting time, average turnaround time,
 * average response time and throughput — from the finished vehicles of a run. Computing strictly
 * from the recorded timestamps (rather than from running counters) keeps the figures reproducible
 * and unit-testable against hand-computed Gantt charts.
 */
public final class MetricsCalculator {

    /**
     * Builds the {@link RunMetrics} for one algorithm.
     *
     * @param algorithmName human-readable algorithm name
     * @param algorithmKey  compact key for charts/CSV (e.g. {@code "RR"})
     * @param dataset       every vehicle in the run (finished and unfinished)
     * @param gantt         the recorded Gantt timeline
     * @param makespan      total ticks the run took
     */
    public RunMetrics calculate(String algorithmName, String algorithmKey,
                                List<Vehicle> dataset, List<GanttSegment> gantt, int makespan) {
        List<VehicleMetrics> rows = new ArrayList<>();
        for (Vehicle v : dataset) {
            if (v.completionTime() == Vehicle.UNSET) {
                continue; // never finished within the run; excluded from averages
            }
            int turnaround = v.completionTime() - v.arrivalTime();
            int waiting = turnaround - v.burstTime();
            int response = v.firstStartTime() - v.arrivalTime();
            rows.add(new VehicleMetrics(
                    v.id(), v.laneId(), v.arrivalTime(), v.burstTime(),
                    v.firstStartTime(), v.completionTime(),
                    waiting, turnaround, response, v.isEmergency()));
        }
        return new RunMetrics(algorithmName, algorithmKey, rows, gantt, dataset.size(), makespan);
    }
}
