/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 *
 * @author adamc
 */
public class GsetEvent extends MEvent {

    // How long during this Event was the cavity in use with RF on and beam present.
    private Duration inUse = null;
    
    // The total trip rate - i.e., the number of FaultAnalyzer trips divided by the GSET "in use" duration
    private Double tripRate = null;

    // How much time was a cavity in use with RF On and CW beam present
    public Duration getInUse() {
        return inUse;
    }

    public void setInUse(Duration inUse) {
        this.inUse = inUse;
    }

    public Double getTripRate() {
        return tripRate;
    }

    public void setTripRate(Double tripRate) {
        this.tripRate = tripRate;
    }

    public GsetEvent(String name, Instant start, Instant end, float value) {
        super(name, start, end, value);
    }

    public GsetEvent(MEvent m) {
        super(m.getName(), m.getStart(), m.getEnd(), m.getValue());
    }

    @Override
    public String toString() {
        return "\name: " + getName()
                + "\nstart: " + getStart()
                + "\nend:   " + getEnd()
                + "\nduration: " + getDuration().toHours()
                + "\nvalue: " + getValue()
                + "\n";
    }

    public static String getCSVHeader() {
        return "PV,Value,Start,End,Duration (minutes),RF On & Beam Present Duration (minutes),Trip Rate (per hr)";
    }

    public String toCSVString() {
        return getName() + "," + getValue()
                + "," + getStart().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "," + getEnd().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + "," + getDuration().toMinutes()
                + "," + inUse.toMinutes()
                + "," + tripRate;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GsetEvent) {
            GsetEvent b = (GsetEvent) o;
            return super.equals(o) && Double.compare(tripRate, b.getTripRate()) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + super.hashCode();
        hash = 61 * hash + Objects.hashCode(this.inUse);
        hash = 61 * hash + Objects.hashCode(this.tripRate);
        return hash;
    }

}
