package com.trafficsim.metrics;

import java.util.List;

/**
 * Aggregated result of running one scheduling algorithm over the dataset: the headline averages
 * used in the comparison dashboard, plus the per-vehicle detail and the Gantt timeline behind them.
 */
public final class RunMetrics {

    private final String algorithmName;
    private final String algorithmKey;
    private final List<VehicleMetrics> perVehicle;
    private final List<GanttSegment> gantt;
    private final int totalVehicles;
    private final int completedVehicles;
    private final int makespan;
    private final double avgWaitingTime;
    private final double avgTurnaroundTime;
    private final double avgResponseTime;
    private final double throughputPer100Ticks;

    public RunMetrics(String algorithmName, String algorithmKey,
                      List<VehicleMetrics> perVehicle, List<GanttSegment> gantt,
                      int totalVehicles, int makespan) {
        this.algorithmName = algorithmName;
        this.algorithmKey = algorithmKey;
        this.perVehicle = List.copyOf(perVehicle);
        this.gantt = List.copyOf(gantt);
        this.totalVehicles = totalVehicles;
        this.completedVehicles = perVehicle.size();
        this.makespan = makespan;
        this.avgWaitingTime = average(perVehicle, VehicleMetrics::waitingTime);
        this.avgTurnaroundTime = average(perVehicle, VehicleMetrics::turnaroundTime);
        this.avgResponseTime = average(perVehicle, VehicleMetrics::responseTime);
        this.throughputPer100Ticks = makespan == 0 ? 0.0 : (completedVehicles * 100.0) / makespan;
    }

    public String algorithmName() { return algorithmName; }
    public String algorithmKey() { return algorithmKey; }
    public List<VehicleMetrics> perVehicle() { return perVehicle; }
    public List<GanttSegment> gantt() { return gantt; }
    public int totalVehicles() { return totalVehicles; }
    public int completedVehicles() { return completedVehicles; }
    public int makespan() { return makespan; }
    public double avgWaitingTime() { return avgWaitingTime; }
    public double avgTurnaroundTime() { return avgTurnaroundTime; }
    public double avgResponseTime() { return avgResponseTime; }
    public double throughputPer100Ticks() { return throughputPer100Ticks; }

    private static double average(List<VehicleMetrics> rows, java.util.function.ToIntFunction<VehicleMetrics> field) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (VehicleMetrics m : rows) {
            sum += field.applyAsInt(m);
        }
        return (double) sum / rows.size();
    }
}
