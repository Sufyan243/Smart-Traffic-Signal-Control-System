package com.trafficsim.metrics;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Exports the dataset and the per-algorithm metrics to CSV so the figures can be pasted straight
 * into the FYP report instead of screenshotting a window (testable signal for FR-04).
 *
 * <p>Three files are written into the chosen directory:</p>
 * <ul>
 *   <li>{@code dataset.csv} — the seeded vehicle workload that every algorithm replayed.</li>
 *   <li>{@code summary.csv} — one row per algorithm with the four headline averages.</li>
 *   <li>{@code per-vehicle.csv} — every vehicle's waiting/turnaround/response under each algorithm.</li>
 *   <li>{@code gantt.csv} — the crossing-order timeline (Gantt segments) of every algorithm.</li>
 * </ul>
 */
public final class ComparisonReportGenerator {

    /** Writes all CSV files for {@code results} into {@code outputDir}; returns the directory. */
    public Path export(Path outputDir, ScenarioConfig config,
                       List<Vehicle> dataset, List<RunMetrics> results) throws IOException {
        Files.createDirectories(outputDir);
        writeDataset(outputDir.resolve("dataset.csv"), config, dataset);
        writeSummary(outputDir.resolve("summary.csv"), config, results);
        writePerVehicle(outputDir.resolve("per-vehicle.csv"), results);
        writeGantt(outputDir.resolve("gantt.csv"), results);
        return outputDir;
    }

    private void writeDataset(Path file, ScenarioConfig config, List<Vehicle> dataset) throws IOException {
        try (Writer w = newWriter(file)) {
            w.write("# seed=" + config.seed() + ", lanes=" + config.laneCount()
                    + ", duration=" + config.durationTicks() + ", quantum=" + config.quantum() + "\n");
            w.write("vehicle_id,lane,arrival_time,burst_time,base_priority,type,emergency\n");
            for (Vehicle v : dataset) {
                w.write("%d,%d,%d,%d,%d,%s,%s%n".formatted(
                        v.id(), v.laneId(), v.arrivalTime(), v.burstTime(),
                        v.basePriority(), v.type().label(), v.isEmergency()));
            }
        }
    }

    private void writeSummary(Path file, ScenarioConfig config, List<RunMetrics> results) throws IOException {
        try (Writer w = newWriter(file)) {
            w.write("# Identical dataset (seed=" + config.seed() + ") replayed across all algorithms\n");
            w.write("algorithm,completed,total,makespan,avg_waiting,avg_turnaround,avg_response,throughput_per_100_ticks\n");
            for (RunMetrics r : results) {
                w.write("%s,%d,%d,%d,%s,%s,%s,%s%n".formatted(
                        r.algorithmKey(), r.completedVehicles(), r.totalVehicles(), r.makespan(),
                        fmt(r.avgWaitingTime()), fmt(r.avgTurnaroundTime()),
                        fmt(r.avgResponseTime()), fmt(r.throughputPer100Ticks())));
            }
        }
    }

    private void writePerVehicle(Path file, List<RunMetrics> results) throws IOException {
        try (Writer w = newWriter(file)) {
            w.write("algorithm,vehicle_id,lane,arrival,burst,first_start,completion,waiting,turnaround,response,emergency\n");
            for (RunMetrics r : results) {
                for (VehicleMetrics m : r.perVehicle()) {
                    w.write("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s%n".formatted(
                            r.algorithmKey(), m.vehicleId(), m.laneId(), m.arrivalTime(), m.burstTime(),
                            m.firstStartTime(), m.completionTime(), m.waitingTime(),
                            m.turnaroundTime(), m.responseTime(), m.emergency()));
                }
            }
        }
    }

    private void writeGantt(Path file, List<RunMetrics> results) throws IOException {
        try (Writer w = newWriter(file)) {
            w.write("algorithm,vehicle_id,lane,type,start_tick,end_tick,duration\n");
            for (RunMetrics r : results) {
                for (GanttSegment s : r.gantt()) {
                    w.write("%s,%d,%d,%s,%d,%d,%d%n".formatted(
                            r.algorithmKey(), s.vehicleId(), s.laneId(), s.type().label(),
                            s.startTick(), s.endTick(), s.duration()));
                }
            }
        }
    }

    private static Writer newWriter(Path file) throws IOException {
        return Files.newBufferedWriter(file, StandardCharsets.UTF_8);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
