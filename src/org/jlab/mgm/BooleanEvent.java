/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 *
 * @author adamc
 */
public class BooleanEvent {

    private final Instant start;
    private final Instant end;
    private final Duration duration;
    private final boolean value;

    public BooleanEvent(Instant start, Instant end, boolean value) {
        this.start = start;
        this.end = end;
        this.duration = Duration.between(start, end);
        this.value = value;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "\nstart: " + start
                + "\nend:   " + end
                + "\nduration: " + duration.toHours()
                + "\nvalue: " + value
                + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BooleanEvent) {
            BooleanEvent b = (BooleanEvent) o;
            return start.equals(b.getStart()) && end.equals(b.getEnd()) && duration.equals(b.getDuration()) && value == b.getValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.start);
        hash = 47 * hash + Objects.hashCode(this.end);
        hash = 47 * hash + Objects.hashCode(this.duration);
        hash = 47 * hash + (this.value ? 1 : 0);
        return hash;
    }
}
