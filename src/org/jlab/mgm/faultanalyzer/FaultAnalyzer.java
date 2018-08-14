/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm.faultanalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author adamc
 */
public class FaultAnalyzer {

    private static final String ENV_DATA_FILE = System.getenv("FAULT_ANALYZER_DATA_FILE");

    public static String rfToFaName(String cavName) {
        char[] n = cavName.toCharArray();
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String zone = n[1] + "L";
        if (allChars.indexOf(n[2]) == -1) {
            zone = zone + "0" + n[2];
        } else {
            int zoneNum = allChars.indexOf(n[2]) + 10;  // 9 digits before the letters, plus 1 for zero-indexed array
            zone = zone + Integer.toString(zoneNum);
        }
        zone = zone + n[3];
        return zone;
    }

    public static TripMap getTripMap() throws IOException {
        String dataFile = ENV_DATA_FILE;
        if (dataFile == null) {
//            dataFile = "C:\\Users\\adamc\\Documents\\FaultAnalyzer-data-20160601-20180801.txt";
            dataFile = "C:\\Users\\adamc\\Documents\\NetBeansProjects\\maxGsetFinder\\config\\FaultAnalyzer-data-20160601-20180801.txt";
        }

        TripMap out = new TripMap();
        Path dataPath = Paths.get(dataFile);
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(dataPath, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                Trip t = Trip.fromDataLine(line);
                out.addTrip(t);
            }
        }
        return out;
    }
}
