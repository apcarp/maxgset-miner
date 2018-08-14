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
public class MEvent {

    private final String name;
    private final Instant start;
    private final Instant end;
    private final Duration duration;
    private final float value;

    public MEvent(String name, Instant start, Instant end, float value) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.duration = Duration.between(start, end);
        this.value = value;
    }

    public String getName() {
        return name;
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

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "\name: " + name
                + "\nstart: " + start
                + "\nend:   " + end
                + "\nduration: " + duration.toHours()
                + "\nvalue: " + value
                + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MEvent) {
            MEvent b = (MEvent) o;
            return start.equals(b.getStart()) && end.equals(b.getEnd()) && getDuration().equals(b.getDuration()) 
                    && getValue() == b.getValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + Objects.hashCode(this.start);
        hash = 83 * hash + Objects.hashCode(this.end);
        hash = 83 * hash + Objects.hashCode(this.duration);
        hash = 83 * hash + Float.floatToIntBits(this.value);
        return hash;
    }
}
