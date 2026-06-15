package com.trafficsim.metrics;

import com.trafficsim.domain.ScenarioConfig;
import com.trafficsim.domain.Vehicle;
import com.trafficsim.scenario.ScenarioManager;
import com.trafficsim.scenario.VehicleTypeSelector;
import com.trafficsim.scheduling.SchedulingAlgorithmSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-10: the report generator must export the dataset and per-algorithm metrics (plus the Gantt
 * timeline) as CSV files with the expected headers and content.
 */
class ComparisonReportGeneratorTest {

    @Test
    void exportsAllCsvFiles(@TempDir Path dir) throws IOException {
        ScenarioConfig config = ScenarioConfig.builder().seed(99L).durationTicks(30).build();
        ScenarioManager manager = new ScenarioManager(new VehicleTypeSelector());
        List<Vehicle> dataset = manager.regenerate(config);
        List<RunMetrics> results =
                new AlgorithmComparator(new SchedulingAlgorithmSelector(), manager).compareAll(dataset, config);

        Path out = new ComparisonReportGenerator().export(dir, config, dataset, results);

        for (String name : List.of("dataset.csv", "summary.csv", "per-vehicle.csv", "gantt.csv")) {
            Path file = out.resolve(name);
            assertTrue(Files.exists(file), name + " should be written");
            assertTrue(Files.size(file) > 0, name + " should not be empty");
        }

        List<String> summary = Files.readAllLines(out.resolve("summary.csv"));
        assertTrue(summary.stream().anyMatch(l -> l.startsWith("algorithm,")), "summary has a header row");
        // one comment line + header + one row per algorithm
        long dataRows = summary.stream().filter(l -> l.matches("^(FCFS|SJF|RR|PRIO),.*")).count();
        assertEquals(4, dataRows, "summary should have one row per algorithm");

        List<String> ganttHeader = Files.readAllLines(out.resolve("gantt.csv"));
        assertEquals("algorithm,vehicle_id,lane,type,start_tick,end_tick,duration", ganttHeader.get(0));
    }
}
