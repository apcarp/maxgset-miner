/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import org.jlab.mgm.FilterMap;
import org.jlab.mgm.BooleanEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NavigableMap;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author adamc
 */
public class FilterMapTest {

    Instant t1, t2, t3, t4, t5, t6, t7, t8, end, subStart, subEnd;
    private final FilterMap m1 = new FilterMap();
    private final FilterMap m2 = new FilterMap();

    public FilterMapTest() {
        t1 = LocalDateTime.parse("2017-01-01T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t2 = LocalDateTime.parse("2017-01-02T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        subStart = LocalDateTime.parse("2017-01-02T12:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t3 = LocalDateTime.parse("2017-01-03T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t4 = LocalDateTime.parse("2017-01-04T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t5 = LocalDateTime.parse("2017-01-05T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t6 = LocalDateTime.parse("2017-01-06T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        subEnd = LocalDateTime.parse("2017-01-06T12:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t7 = LocalDateTime.parse("2017-01-07T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        t8 = LocalDateTime.parse("2017-01-08T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        end = LocalDateTime.parse("2017-02-01T00:00:00").atZone(ZoneId.systemDefault()).toInstant();

        m1.addEvent(new BooleanEvent(t1, t3, true));
        m1.addEvent(new BooleanEvent(t3, t5, false));
        m1.addEvent(new BooleanEvent(t5, t7, true));
        m1.addEvent(new BooleanEvent(t7, end, false));

        m2.addEvent(new BooleanEvent(t2, t4, true));
        m2.addEvent(new BooleanEvent(t4, t6, false));
        m2.addEvent(new BooleanEvent(t6, t8, true));
        m2.addEvent(new BooleanEvent(t8, end, false));
    }

    /**
     * Test of addEvent method, of class FilterMap.
     */
//    @Test
//    public void testAddEvent() {
//        System.out.println("addEvent");
//        BooleanEvent event = null;
//        FilterMap instance = new FilterMap();
//        instance.addEvent(event);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of updateThresholdMap method, of class FilterMap.
     */
//    @Test
//    public void testUpdateFilterMap() {
//        System.out.println("updateThresholdMap");
//        Instant i = null;
//        BooleanEvent b = null;
//        NavigableMap<Instant, BooleanEvent> m = null;
//        String operation = "";
//        FilterMap instance = new FilterMap();
//        instance.updateFilterMap(b, m, operation);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getTrueDuration method, of class FilterMap
     */
    @Test
    public void testGetTrueDuration(){
        System.out.println("getTrueDuration");
        Duration expResult = Duration.ofDays(2);
        Duration result = m1.getTrueDuration(subStart, subEnd);
        assertEquals(expResult, result);
        System.out.println(" - PASSED");
    }
    
    /**
     * Test of combineFilterMap method, of class FilterMap.
     */
    @Test
    public void testCombineFilterMap() {
        System.out.println("combineThresholdMaps");

        System.out.print("  Or Combination");
        FilterMap orResult = FilterMap.combineFilterMaps(m1, m2, "or");
        FilterMap expOrResult = new FilterMap();
        expOrResult.addEvent(new BooleanEvent(t1, t4, true));
        expOrResult.addEvent(new BooleanEvent(t4, t5, false));
        expOrResult.addEvent(new BooleanEvent(t5, t8, true));
        expOrResult.addEvent(new BooleanEvent(t8, end, false));
        assertEquals(expOrResult, orResult);
        System.out.println(" - PASSED");

        System.out.print("  And Combination");
        FilterMap andResult = FilterMap.combineFilterMaps(m1, m2, "and");
        FilterMap expAndResult = new FilterMap();
        expAndResult.addEvent(new BooleanEvent(t1, t3, true));
        expAndResult.addEvent(new BooleanEvent(t3, t6, false));
        expAndResult.addEvent(new BooleanEvent(t6, t7, true));
        expAndResult.addEvent(new BooleanEvent(t7, end, false));
        System.out.println(" - PASSED");

        assertEquals(expAndResult, andResult);
    }
}
