/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm.faultanalyzer;

import org.jlab.mgm.faultanalyzer.TripMap;
import org.jlab.mgm.faultanalyzer.FaultAnalyzer;
import org.jlab.mgm.FilterMap;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jlab.mgm.BooleanEvent;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author adamc
 */
public class FaultAnalyzerTest {

    public FaultAnalyzerTest() {
    }

    /**
     * Test of rfToFaName method, of class FaultAnalyzer.
     */
    @Test
    public void testRfToFaName() {
        System.out.println("rfToFaName");
        String[] cavNames = {"R123", "R024", "R2Q8", "R1M5"};
        String[] expResult = {"1L023", "0L024", "2L268", "1L225"};
        String[] result = new String[4];
        for (int i = 0; i < 4; i++) {
            result[i] = FaultAnalyzer.rfToFaName(cavNames[i]);
        }
        Assert.assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of getTripRate method, of class FaultAnalyzer.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testGetNumTrips() throws IOException {
        System.out.println("getTripRate");
        String cavName = "R121";

        Instant f1 = LocalDateTime.parse("05/01/2018 00:00:00", DateTimeFormatter.ofPattern("M/d/y H:m:s")).atZone(ZoneId.systemDefault()).toInstant();
        Instant f2 = LocalDateTime.parse("05/10/2018 00:00:00", DateTimeFormatter.ofPattern("M/d/y H:m:s")).atZone(ZoneId.systemDefault()).toInstant();

        // These the trips that should be seen between start and end.  We should only get the two trips back.  (Segment from trip file)
        //            "1L021	05/02/2018 12:39:33	72	0.89	6.08	3608123973	 6.08 8.40 8.01 7.80 8.18 7.00 7.50 7.44 	198658480	Ready",
        //            "1L021	05/05/2018 14:48:06	72	1.06	5.71	3608390886	 5.71 8.40 7.76 7.80 7.84 7.00 7.33 7.20 	198866000	Ready",
        //            "1L021	08/01/2018 21:02:53	72	1.25	7.20	3616016573	 7.20 10.70 8.50 7.60 8.60 8.00 8.70 0.00 	199085245	Maintenance"
        //            "1L021	08/01/2018 21:40:19	64	0.01	7.20	3616018819	 7.20 10.70 8.50 7.60 8.70 8.00 8.70 0.00 	199087475	Maintenance"
        TripMap tripMap = FaultAnalyzer.getTripMap();
        int expResult = 2;
        int result = tripMap.getNumTrips(cavName, f1, f2);

        assertEquals(expResult, result);

    }
}
