package com.trafficsim.domain;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * One approach to the intersection. A lane is mainly a visualization grouping: it owns the queue of
 * vehicles waiting at its stop line and the colour of its signal. Scheduling itself is global across
 * all vehicles (see the scheduling package); whichever vehicle currently holds the intersection
 * turns its lane {@link SignalState#GREEN} while every other lane is {@link SignalState#RED}.
 */
public final class Lane {

    private final int id;
    private final String name;
    private final Deque<Vehicle> queue = new ArrayDeque<>();
    private SignalState signal = SignalState.RED;

    public Lane(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() { return id; }
    public String name() { return name; }

    public SignalState signal() { return signal; }
    public void setSignal(SignalState signal) { this.signal = signal; }

    public void enqueue(Vehicle v) { queue.addLast(v); }
    public void remove(Vehicle v) { queue.remove(v); }
    public int queueLength() { return queue.size(); }
    public boolean isEmpty() { return queue.isEmpty(); }

    /** Read-only snapshot of the current queue, used by the renderer. */
    public List<Vehicle> snapshot() {
        return List.copyOf(queue);
    }

    public void clear() {
        queue.clear();
        signal = SignalState.RED;
    }

    public static List<Lane> createLanes(int count) {
        String[] names = {"North", "East", "South", "West", "NE", "SE", "SW", "NW"};
        Lane[] lanes = new Lane[count];
        for (int i = 0; i < count; i++) {
            lanes[i] = new Lane(i, i < names.length ? names[i] : "Lane " + i);
        }
        return Collections.unmodifiableList(List.of(lanes));
    }
}
