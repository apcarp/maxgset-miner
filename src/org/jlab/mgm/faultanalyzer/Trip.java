/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm.faultanalyzer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 *
 * @author adamc
 */
public class Trip {

    private final String cavName;
    private final Instant time;
    private final double crfp;
    private final double gset;
    private final long onTimer;

    public static Trip fromDataLine(String line) {
        // Example line from compiled data file.  C100 trips do no include the RF_state or corr_time fields, but they are the after any we need to parse
        //#cavity	date	time	flt2	fwd_pwr	gset	flttime	cav1grad	cav2grad	cav3grad	cav4grad	cav5grad	cav6grad	cav7grad	cav8grad	corr_time	RF_state	event
        //0L031	06/10/2016	02:06:25	72	2.12	8.50	3548383585	8.50	9.00	10.00	7.50	3.05	3.05	11.00	0.00	30593070	Ready	CavityEvent8
        String[] tokens = line.split("\\s+");
        return new Trip(tokens[0],  // cavName
                LocalDateTime.parse(tokens[1] + " " + tokens[2], DateTimeFormatter.ofPattern("M/d/y H:m:s")).atZone(ZoneId.systemDefault()).toInstant(),  // timestamp
//                Integer.valueOf(tokens[3]),  // tripCode
                tokens[4].equals("fail") ? Double.NaN : Double.valueOf(tokens[4]),   // fwd_pwr
                Double.valueOf(tokens[5]),   // gset
                Long.valueOf(tokens[15])   // corr_time (equivalent to RF On PV time in modern times (2018)
        );
    }

    public Trip(String cavName, Instant time, double crfp, double gset, long onTimer) {
        this.cavName = cavName;
        this.time = time;
        this.crfp = crfp;
        this.gset = gset;
        this.onTimer = onTimer;
    }

    public String getCavName() {
        return cavName;
    }

    public Instant getTime() {
        return time;
    }

    public double getCrfp() {
        return crfp;
    }

    public double getGset() {
        return gset;
    }

    public long getOnTimer() {
        return onTimer;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Trip) {
            Trip t = (Trip) o;
            return cavName.equals(t.getCavName()) && time.equals(t.getTime()) && crfp == t.getCrfp()
                    && gset == t.getGset() && onTimer == t.getOnTimer();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.cavName);
        hash = 59 * hash + Objects.hashCode(this.time);
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.crfp) ^ (Double.doubleToLongBits(this.crfp) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.gset) ^ (Double.doubleToLongBits(this.gset) >>> 32));
        hash = 59 * hash + (int) (this.onTimer ^ (this.onTimer >>> 32));
        return hash;
    }


}
