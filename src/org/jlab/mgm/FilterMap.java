/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jlab.mgm.BooleanEvent;

/**
 *
 * @author adamc
 */
public class FilterMap extends TreeMap<Instant, BooleanEvent> {

    public FilterMap() {
    }

    public FilterMap(FilterMap m) {
        putAll(m);
    }

    /**
     * Return the amount of time that the FilterMap was in a true state between
     * start and end.
     *
     * @param start
     * @param end
     * @return
     */
    public Duration getTrueDuration(Instant start, Instant end) {
        Duration duration = Duration.ZERO;
        if (floorKey(start) != null && floorKey(start).isBefore(start)) {
            BooleanEvent prior = floorEntry(start).getValue();
            if (prior != null && prior.getValue()) {
                duration = duration.plus(Duration.between(start, prior.getEnd()));
            }
        }
        SortedMap<Instant, BooleanEvent> sub = subMap(start, end);

        for (Entry<Instant, BooleanEvent> e : sub.entrySet()) {
            BooleanEvent b = e.getValue();
            if (b.getValue()) {
                if (b.getStart().isAfter(end)) {
                    throw new RuntimeException("Submap contains point starting after end");
                } else if (b.getEnd().isAfter(end)) {
                    // The last point may extend beyond the request end point.  Manually calculate the duration of this point during
                    // the sub map period
                    duration = duration.plus(Duration.between(b.getStart(), end));
                } else {
                    // The point starts and ends with the requested bounds.  Just query the duration of point.
                    duration = duration.plus(b.getDuration());
                }
            }
        }
        return duration;
    }

    /**
     * Add an event to the map. If you are inserting after an existing event,
     * you need to update the end and duration of that previous event since the
     * time line has now changed. This method assumes that you are always adding
     * events to the end of the map, and so do not have to add points after the
     * new event to fill the gap between the end of the new event and the start
     * of the next event.
     *
     * Note: This also changes the end point of the previous point to be the
     * start of the currently added event.
     *
     * @param event
     */
    public void addEvent(BooleanEvent event) {
        Instant start = event.getStart();
        if (isEmpty()) {
            put(start, event);
        } else {
            // The end and duration fields we put here for the new point will almost certainly be wrong.  First correct the previous
            // point's end and duration fields since we now know the correct information for that point.  Then add the point under 
            // consideration.
            BooleanEvent prev = floorEntry(start).getValue();
            put(floorKey(start), new BooleanEvent(prev.getStart(), start, prev.getValue()));

            // Now add the point
            put(start, event);

            if (higherKey(start) != null) {
                throw new RuntimeException("Insertion in middle of this map is not currently allowed.");
            }
        }
    }

    private void updateFilterMapOr(BooleanEvent b1, NavigableMap<Instant, BooleanEvent> m2) {
        Instant i1 = b1.getStart();
        if (floorEntry(i1) == null) {
            // this is the very first point so add it to the output map
            addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), b1.getValue()));
        } else if (b1.getValue() || (m2.floorEntry(i1) != null && m2.floorEntry(i1).getValue().getValue())) {

            // if b1 is true or the value of m2 at i1 exists and is true 
            if (!floorEntry(i1).getValue().getValue()) {
                // If the output at i1 is false, add a true value at i1 - this check only adds needed values
                addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), true));
            }
        } else if (!b1.getValue() && (m2.floorEntry(i1) == null || !m2.floorEntry(i1).getValue().getValue())) {
            // if b1 is false and either m2 doesn't exist or m2's value is also false
            if (floorEntry(i1).getValue().getValue()) {
                // If the output at i1 is currently true, add a false value at i1 - this check only adds neede values
                addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), false));
            }
        }
    }

    private void updateFilterMapAnd(BooleanEvent b1, NavigableMap<Instant, BooleanEvent> m2) {
        Instant i1 = b1.getStart();
        if (floorEntry(i1) == null) {
            // this is the very first point so add it to the output map
            addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), b1.getValue()));
        } else if (b1.getValue()) {
            if (m2.floorEntry(i1) == null || m2.floorEntry(i1).getValue().getValue()) {
                // if b1 is true and the value of m2 at i1 exists and is true - might have to and a true value
                if (!floorEntry(i1).getValue().getValue()) {
                    // If the output at i1 is false, add a true value at i1 - this check only adds needed values
                    addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), true));
                }
            } else if (!m2.floorEntry(i1).getValue().getValue()) {
                // if the m2 value at b1 is false, then we might have to add a false value
                if (floorEntry(i1).getValue().getValue()) {
                    // If the output at i1 is false, add a true value at i1 - this check only adds needed values
                    addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), false));
                }
            }
        } else if (!b1.getValue()) {

            // if b1 is false then update the output.  Don't bother checking m2 since they both have to be true for out to be true
            if (floorEntry(i1).getValue().getValue()) {
                // If the output at i1 is currently true, add a false value at i1 - this check only adds neede values
                addEvent(new BooleanEvent(b1.getStart(), b1.getEnd(), false));
            }
        }
    }

    public void updateFilterMap(BooleanEvent b, NavigableMap<Instant, BooleanEvent> m, String operation) {
        switch (operation) {
            case "or":
                updateFilterMapOr(b, m);
                break;
            case "and":
                updateFilterMapAnd(b, m);
                break;
            default:
                throw new RuntimeException("Unrecognized combine operation:" + operation);
        }

    }

    public static FilterMap combineFilterMap(List<FilterMap> maps, String operation) {
        FilterMap out;
        Iterator<FilterMap> it = maps.iterator();
        if (it.hasNext()) {
            out = it.next();
        } else {
            return null;
        }
        while(it.hasNext()) {
            out = FilterMap.combineFilterMaps(out, it.next(), operation);
        }
        return out;
    }
    
    /*
    Here we iterate down the two maps, adding the older event from either map one at a time to the combined map.
     */
    public static FilterMap combineFilterMaps(FilterMap m1, FilterMap m2, String operation) {

        Iterator<Entry<Instant, BooleanEvent>> it1 = m1.entrySet().iterator();
        Iterator<Entry<Instant, BooleanEvent>> it2 = m2.entrySet().iterator();

        // Get the first two entrys.  If either is null, then we will essentially return the non-null map.
        Entry<Instant, BooleanEvent> e1 = it1.hasNext() ? it1.next() : null;
        Entry<Instant, BooleanEvent> e2 = it2.hasNext() ? it2.next() : null;

        // Handle some corner cases where we were passed empty maps, then get on with the main business of combining to 
        // "good" maps
        if (e1 == null && e2 != null) {
            return new FilterMap(m2);
        } else if (e1 != null && e2 == null) {
            return new FilterMap(m1);
        } else if (e1 == null && e2 == null) {
            return new FilterMap();
        } else {
            FilterMap out = new FilterMap();
            if (e1 == null || e2 == null) {
                // Mostly to make the IDE happy
                throw new RuntimeException("Received null object where not expecting");
            }

            Instant i1 = e1.getKey();
            Instant i2 = e2.getKey();
            BooleanEvent b1 = e1.getValue();
            BooleanEvent b2 = e2.getValue();

            while (e1 != null && e2 != null) {
                if (i1 == null || i2 == null || b1 == null || b2 == null) {
                    throw new RuntimeException("Received null object where not expecting");
                }
                if (!i1.isAfter(i2)) {
                    out.updateFilterMap(b1, m2, operation);
                    // Move e1/i1/b1 on to the next value of it1 or set them to null
                    if (it1.hasNext()) {
                        e1 = it1.next();
                        i1 = e1.getKey();
                        b1 = e1.getValue();
                    } else {
                        e1 = null;
                        i1 = null;
                        b1 = null;
                    }
                } else { // i2 is before or equal to i1
                    out.updateFilterMap(b2, m1, operation);
                    // Move e1/i1/b1 on to the next value of it1 or set them to null
                    if (it2.hasNext()) {
                        e2 = it2.next();
                        i2 = e2.getKey();
                        b2 = e2.getValue();
                    } else {
                        e2 = null;
                        i2 = null;
                        b2 = null;
                    }
                }
            }

            // There will be one hanging element from above.  Process that and then run through the rest of the points via the iterator
            if (e1 != null && i1 != null && b1 != null) {
                out.updateFilterMap(b1, m2, operation);
            }
            if (e2 != null && i2 != null && b2 != null) {
                out.updateFilterMap(b2, m1, operation);
            }

            // Now only one of the event set iterators has anything left, and only one of these loops should do anything.
            while (it1.hasNext()) {
                e1 = it1.next();
                b1 = e1.getValue();
                out.updateFilterMap(b1, m2, operation);
            }
            while (it2.hasNext()) {
                e2 = it2.next();
                b2 = e2.getValue();
                out.updateFilterMap(b2, m1, operation);
            }

            // The updateThreshdoldMap updates the previous point's duration and end time when the next point is added.  We have to
            // do that manually now since there will be no more calls.
            Instant m1End = m1.lastEntry().getValue().getEnd();
            Instant m2End = m2.lastEntry().getValue().getEnd();
            Instant end = m1End.isAfter(m2End) ? m1End : m2End;
            BooleanEvent last = out.floorEntry(end).getValue();
            out.put(out.floorKey(end), new BooleanEvent(last.getStart(), end, last.getValue()));

            return out;
        }
    }
}
