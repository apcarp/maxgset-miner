/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import org.jlab.mgm.MaxGsetFinder;
import org.jlab.mgm.MEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jlab.mya.DataNexus;
import org.jlab.mya.Deployment;
import org.jlab.mya.Metadata;
import org.jlab.mya.nexus.OnDemandNexus;
import org.jlab.mya.service.IntervalService;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author adamc
 */
public class MaxGsetFinderTest {

    public MaxGsetFinderTest() {
    }

    /**
     * Test of main method, of class MaxGsetFinder.
     */
//    @Test
//    public void testMain() throws Exception {
//        System.out.println("main");
//        String[] args = null;
//        MaxGsetFinder.main(args);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getPvMap method, of class MaxGsetFinder.
     */
    @Test
    public void testGetPvMap() throws Exception {
        System.out.println("getPvMap");

        Instant begin = LocalDateTime.parse("2018-08-09T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.parse("2018-08-10T04:32:00").atZone(ZoneId.systemDefault()).toInstant();
        Instant t1 = LocalDateTime.parse("2018-08-10T04:31:26.561230627").atZone(ZoneId.systemDefault()).toInstant();
        Instant t2 = LocalDateTime.parse("2018-08-10T04:31:30.277897145").atZone(ZoneId.systemDefault()).toInstant();
        Instant t3 = LocalDateTime.parse("2018-08-10T04:31:42.894959097").atZone(ZoneId.systemDefault()).toInstant();
        String eventType = "float";
        String pv = "R123GSET";
        
        DataNexus nexus = new OnDemandNexus(Deployment.ops);
        IntervalService service = new IntervalService(nexus);
        Metadata meta = service.findMetadata(pv);
        
        
        NavigableMap<Instant, MEvent> expResult = new TreeMap<>();
        expResult.put(t1, new MEvent(pv, t1, t2, 5.3f));
        expResult.put(t2, new MEvent(pv, t2, t3, 5f));
        expResult.put(t3, new MEvent(pv, t3, end, 2f));
        
        NavigableMap<Instant, MEvent> result = MaxGsetFinder.getPvMap(service, pv, begin, end, eventType);
        
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
    }
}
