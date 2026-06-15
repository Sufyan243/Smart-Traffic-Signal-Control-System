package com.trafficsim.domain;

/**
 * Virtual, tick-based clock that decouples simulation time from wall-clock time. Every algorithm
 * advances this clock one discrete tick at a time, which is what makes runs reproducible: the same
 * dataset always produces the same sequence of events regardless of how fast the UI animates it.
 */
public final class SimClock {

    private int tick;

    public int currentTick() {
        return tick;
    }

    public int advance() {
        return ++tick;
    }

    public void reset() {
        tick = 0;
    }
}
