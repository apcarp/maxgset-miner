/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm.faultanalyzer;

import org.jlab.mgm.faultanalyzer.Trip;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author adamc
 */
public class TripTest {
    
    public TripTest() {
    }

    /**
     * Test of fromDataLine method, of class Trip.
     */
    @Test
    public void testFromDataLine() {
        System.out.println("fromDataLine");
        String line = "1L023	05/05/2018 04:53:20	72	1.64	7.76	3608355200	 5.71 8.40 7.76 7.80 7.84 7.00 7.33 7.20 	215671320	Ready";
        Instant time = LocalDateTime.parse("05/05/2018 04:53:20", DateTimeFormatter.ofPattern("M/d/y H:m:s")).atZone(ZoneId.systemDefault()).toInstant();
        Trip expResult = new Trip("1L023", time, 1.64, 7.76, 215671320);
        Trip result = Trip.fromDataLine(line);
        assertEquals(expResult, result);
    }
}
