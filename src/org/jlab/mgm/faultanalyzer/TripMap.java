/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm.faultanalyzer;

import java.time.Instant;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class is essentially a specialized data class for holding the collection
 * of trip data lookups happen first by cavity name and then in chronological
 * order.
 *
 * @author adamc
 */
public class TripMap extends HashMap<String, TreeMap<Instant, Trip>> {

    public void addTrip(Trip t) {
        String name = t.getCavName();
        if (get(name) == null) {
            put(name, new TreeMap<>());
        }
        get(name).put(t.getTime(), t);
    }

    public int getNumTrips(String cavity, Instant start, Instant end) {
        String name = FaultAnalyzer.rfToFaName(cavity);
        if (get(name) == null ) {
            return 0;
        }
        return get(name).subMap(start, end).size();
    }    
}
