package com.trafficsim.scheduling;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registry and factory for the available scheduling algorithms (testable signal for FR-02).
 *
 * <p>The engine and UI obtain strategies exclusively through this selector, so adding a new
 * algorithm is a one-line change here and nothing else has to know about it. Each request returns a
 * <em>fresh</em> strategy instance, which matters because strategies are stateful across a run.</p>
 */
public final class SchedulingAlgorithmSelector {

    /** One selectable algorithm: a stable key, display name and a factory for fresh instances. */
    public record Option(String key, String displayName, Supplier<SchedulingStrategy> factory) {
        public SchedulingStrategy create() {
            return factory.get();
        }
    }

    private static final List<Option> OPTIONS = List.of(
            new Option("FCFS", "First-Come First-Served", FcfsStrategy::new),
            new Option("SJF", "Shortest Job First", SjfStrategy::new),
            new Option("RR", "Round Robin", RoundRobinStrategy::new),
            new Option("PRIO", "Priority + Aging", PriorityAgingStrategy::new)
    );

    public List<Option> options() {
        return OPTIONS;
    }

    /** All algorithms, freshly instantiated — used by the comparison runner. */
    public List<SchedulingStrategy> createAll() {
        return OPTIONS.stream().map(Option::create).toList();
    }

    /** Creates the algorithm identified by {@code key} (e.g. {@code "RR"}). */
    public SchedulingStrategy create(String key) {
        return OPTIONS.stream()
                .filter(o -> o.key().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scheduling algorithm: " + key))
                .create();
    }

    public Option defaultOption() {
        return OPTIONS.get(0);
    }
}
