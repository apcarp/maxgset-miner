/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.mgm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.naming.NamingException;
import org.jlab.mgm.connectionpools.StandaloneConnectionPools;
import org.jlab.mgm.connectionpools.StandaloneJndi;
import org.jlab.mgm.faultanalyzer.FaultAnalyzer;
import org.jlab.mgm.faultanalyzer.TripMap;
import org.jlab.mya.Deployment;
import org.jlab.mya.Event;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.nexus.PooledNexus;
import org.jlab.mya.params.IntervalQueryParams;
import org.jlab.mya.params.PointQueryParams;
import org.jlab.mya.service.PointService;
import org.jlab.mya.stream.FloatEventStream;
import org.jlab.mya.stream.IntEventStream;

/**
 *
 * @author adamc
 */
public class MaxGsetFinder {

    private final static String CED_INVENTORY_URL = "http://ced.acc.jlab.org/inventory";
    private final static String MYGET_URL = "http://myaweb.acc.jlab.org/myget/span-data";

    public static NavigableMap<Instant, MEvent> getPvMap(IntervalService service, String pv, Instant begin, Instant end, String eventType) throws SQLException, IOException {
        NavigableMap<Instant, MEvent> map = new TreeMap<>();

        // Create an easy to work with data structure of the injector gun high voltage state.  1 == on/enabled
        Metadata metadata = service.findMetadata(pv);
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);

        if (eventType.equals("int")) {
            try (IntEventStream stream = service.openIntStream(params)) {
                IntEvent prev, event;
                prev = stream.read();
                if (prev != null) {
                    while ((event = stream.read()) != null) {
                        map.put(prev.getTimestampAsInstant(), new MEvent(pv, prev.getTimestampAsInstant(), event.getTimestampAsInstant(), prev.getValue()));
                        prev = event;
                    }
                    map.put(prev.getTimestampAsInstant(), new MEvent(pv, prev.getTimestampAsInstant(), end, prev.getValue()));
                }
            }
        } else if (eventType.equals("float")) {
            try (FloatEventStream stream = service.openFloatStream(params)) {
                FloatEvent prev, event;
                prev = stream.read();
                if (prev != null) {
                    while ((event = stream.read()) != null) {
                        map.put(prev.getTimestampAsInstant(), new MEvent(pv, prev.getTimestampAsInstant(), event.getTimestampAsInstant(), prev.getValue()));
                        prev = event;
                    }
                    map.put(prev.getTimestampAsInstant(), new MEvent(pv, prev.getTimestampAsInstant(), end, prev.getValue()));
                }
            }
        }

        return map;
    }

    public static boolean eqThreshold(int i1, int i2) {
        return i1 == i2;
    }

    public static boolean geThreshold(double d1, double d2) {
        return d1 >= d2;
    }

    public static boolean ltThreshold(double d1, double d2) {
        return d1 < d2;
    }

    public static boolean neThreshold(double d1, double d2) {
        return d1 != d2;
    }

    // Deteremines whether the value meets the specified threshold.  Returns 1 if so, returns 0 if not.  Throws exception on
    // unrecognized thresholdType.  Note: value and threshold cast to int for "eq"
    public static boolean meetsThreshold(double value, double threshold, String thresholdType) {
        boolean out;
        switch (thresholdType) {
            case "eq":
                out = eqThreshold((int) value, (int) threshold);
                break;
            case "ne":
                out = neThreshold(value, threshold);
                break;
            case "ge":
                out = geThreshold(value, threshold);
                break;
            case "lt":
                out = ltThreshold(value, threshold);
                break;
            default:
                throw new RuntimeException("Unrecognized thresholdType");
        }
        return out;
    }

    // Creates a map of Instant/MEvents where the MEvents values are 1/0 to indicate whether the PV was above/below met the threshold.
    // threshold gets cast to an int if thresholdType is equal.
    public static FilterMap getFilterMap(IntervalService service, String pv, Instant begin, Instant end, String eventType,
            String thresholdType, double threshold) throws SQLException, IOException {
        FilterMap map = new FilterMap();

        // Create an easy to work with data structure of the injector gun high voltage state.  1 == on/enabled
        Metadata metadata = service.findMetadata(pv);
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);

        if (eventType.equals("int")) {
            try (IntEventStream stream = service.openIntStream(params)) {
                IntEvent prev, event;
                prev = stream.read();
                if (prev != null) {
                    boolean prevValue = meetsThreshold((double) prev.getValue(), threshold, thresholdType);
                    boolean value;
                    while ((event = stream.read()) != null) {
                        value = meetsThreshold((double) event.getValue(), threshold, thresholdType);
                        if (prevValue != value) {
                            map.addEvent(new BooleanEvent(prev.getTimestampAsInstant(), event.getTimestampAsInstant(), prevValue));
                            prev = event;
                            prevValue = value;
                        }
                    }

                    // Add the last point and use the "end" time for the duration calculation, not the next event
                    map.addEvent(new BooleanEvent(prev.getTimestampAsInstant(), end, prevValue));
                }
            }
        } else if (eventType.equals("float")) {
            try (FloatEventStream stream = service.openFloatStream(params)) {
                FloatEvent prev, event;
                prev = stream.read();
                if (prev != null) {
                    boolean value;
                    boolean prevValue = meetsThreshold((double) prev.getValue(), threshold, thresholdType);
                    while ((event = stream.read()) != null) {
                        value = meetsThreshold((double) event.getValue(), threshold, thresholdType);
                        if (prevValue != value) {
                            map.addEvent(new BooleanEvent(prev.getTimestampAsInstant(), event.getTimestampAsInstant(), prevValue));
                            prev = event;
                            prevValue = value;
                        }
                    }
                    // Add the last point and use the "end" time for the duration calculation, not the next event
                    map.addEvent(new BooleanEvent(prev.getTimestampAsInstant(), end, prevValue));
                }
            }
        }

        return map;
    }

    public static Event getPoint(PointService service, String pv, Instant time) throws SQLException {
        Metadata metadata = service.findMetadata(pv);
        PointQueryParams params = new PointQueryParams(metadata, time);
        return service.findEvent(params);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException, IOException, InterruptedException, NamingException {

        new StandaloneJndi();
        try (StandaloneConnectionPools pools = new StandaloneConnectionPools(Deployment.opsfb)) {
            PooledNexus nexus = new PooledNexus(Deployment.opsfb);
            IntervalService service = new IntervalService(nexus);

            NavigableMap<Instant, MEvent> hvOn = new TreeMap<>();

            Instant begin = LocalDateTime.parse("2016-10-01T00:00:00").atZone(ZoneId.systemDefault()).toInstant();
            Instant end = LocalDateTime.parse("2018-06-01T00:00:00").atZone(ZoneId.systemDefault()).toInstant();

            List<String> cavityNames = new ArrayList<>(400);
            String[] linacs = {"1", "2"};
            String[] zones = {"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q"};
            String[] cavs = {"1", "2", "3", "4", "5", "6", "7", "8"};
            for (String i : linacs) {
                for (String j : zones) {
                    for (String k : cavs) {
                        cavityNames.add("R" + i + j + k);
                    }
                }
            }

            // Create an easy to work with data structure of the injector gun high voltage state.  1 == on/enabled
            String gunHvPv = "IGL0I00HVONSTAT";
            String bpmPv = "IPM2S01.BNSF";
            String masterModePv = "IGL1I00BEAMODE";

            System.out.println(gunHvPv);
            FilterMap gunHvMap = getFilterMap(service, gunHvPv, begin, end, "int", "eq", 1);
            System.out.println(bpmPv);
            FilterMap bpmBNSFMap = getFilterMap(service, bpmPv, begin, end, "int", "eq", 0);
            System.out.println(masterModePv);
            FilterMap masterModeMap = getFilterMap(service, masterModePv, begin, end, "int", "eq", 3);

            String[] chopperPvs = {"SMRPOSA", "SMRPOSB", "SMRPOSC"};
            List<FilterMap> chopperMaps = new ArrayList<>();
            for (String pv : chopperPvs) {
                chopperMaps.add(getFilterMap(service, pv, begin, end, "float", "ne", 8));
            }

            String[] bcmPvs = {"IBC1H04CRCUR2", "IPM2C24A.IENG", "IBC3H00CRCUR4", "IBCAD00CRCUR6"};
            List<FilterMap> bcmMaps = new ArrayList<>();
            for (String pv : bcmPvs) {
                System.out.println(pv);
                if (pv.equals("IBCAD00CRCUR6")) {
                    bcmMaps.add(getFilterMap(service, pv, begin, end, "float", "ge", 5));
                } else {
                    bcmMaps.add(getFilterMap(service, pv, begin, end, "float", "ge", 1));
                }
            }

            String[] laserPvs = {"IGL1I00HALLAMODE", "IGL1I00HALLBMODE", "IGL1I00HALLCMODE",};
            List<FilterMap> laserMaps = new ArrayList<>();
            for (String pv : laserPvs) {
                System.out.println(pv);
                laserMaps.add(getFilterMap(service, pv, begin, end, "int", "eq", 3));
            }

            // Combine all of the injector filter maps to determine if beam is being produced and not being blocked by the chopper
            FilterMap chopperOpen = FilterMap.combineFilterMap(chopperMaps, "or");
            FilterMap laserCWMode = FilterMap.combineFilterMap(laserMaps, "or");
            laserCWMode = FilterMap.combineFilterMaps(laserCWMode, masterModeMap, "and");
            List<FilterMap> injMaps = new ArrayList<>();
            injMaps.add(chopperOpen);
            injMaps.add(laserCWMode);
            injMaps.add(gunHvMap);
            FilterMap beamAvailable = FilterMap.combineFilterMap(injMaps, "and");

            // Combine the BCM and BPM filter maps to determine if beam is being detected downstream of the south linac.
            FilterMap beamDetected = FilterMap.combineFilterMap(bcmMaps, "or");
            beamDetected = FilterMap.combineFilterMaps(beamDetected, bpmBNSFMap, "or");
            FilterMap beamPresentInLinacs = FilterMap.combineFilterMaps(beamDetected, beamAvailable, "and");

            PointService pService = new PointService(nexus);
            // Hall A pass #, Hall B pass #, Hall C pass #, Hall D pass #, Hall A Current, Hall B Current, Hall C Current, Hall D Current
            String[] gsetMetadataPvs = {"MMSHLAPASS", "MMSHLBPASS", "MMSHLCPASS", "MMSHLDPASS", "IBC1H04CRCUR2",
                "IPM2C24A.IENG", "IBC3H00CRCUR4", "IBCAD00CRCUR6"};

            // Parse the FaultAnalyzer file to get a map of trip by cavity
            TripMap tripMap = FaultAnalyzer.getTripMap();

            // Setup for outping a CSV of results
            List<String> csvContent = new ArrayList<>();
            csvContent.add(GsetEvent.getCSVHeader() + ","
                    + "A Pass Timestamp,A Pass (MMSHLAPASS),"
                    + "B Pass Timestamp,B Pass(MMSHLBPASS),"
                    + "C Pass Timestamp,C Pass (MMSHLCPASS),"
                    + "D Pass Timestamp,D Pass (MMSHLDPASS),"
                    + "A Current Timestamp,A Current (IBC1H04CRCUR2),"
                    + "B Current Timestamp,B Current (IPM2C24A.IENG),"
                    + "C Current Timestamp,C Current (IBC3H00CRCUR4),"
                    + "D Current Timestamp,D Current (IBCAD00CRCUR6)");

            for (String cavName : cavityNames) {
                // Used to short circuit the loop during testing
//            if ( cavName.equals("R123")) {
//                break;
//            }

                System.out.println(cavName + " -- " + LocalDateTime.now());
                String gsetPv = cavName + "GSET";
                String rfOnPv;
                if (cavName.substring(2, 3).matches("[MNOPQ]")) {
                    rfOnPv = cavName + "RFONr"; // C100 RF On PV
                } else {
                    rfOnPv = cavName + "ACK1.B6"; // C25/C50 RF On PV
                }

                NavigableMap<Instant, MEvent> gsetMap = getPvMap(service, gsetPv, begin, end, "float");
                FilterMap rfOnMap = getFilterMap(service, rfOnPv, begin, end, "int", "eq", 1);
                FilterMap beamPresentRfOnMap = FilterMap.combineFilterMaps(rfOnMap, beamPresentInLinacs, "and");

                // Create a treeset that stores MEvents in descending order based on value.  Makes getting the top ten easy.
                SortedSet<GsetEvent> topGsets = new TreeSet<>(new Comparator() {
                    // Return 
                    @Override
                    public int compare(Object o1, Object o2) {
                        GsetEvent m1 = (GsetEvent) o1;
                        GsetEvent m2 = (GsetEvent) o2;
                        return m2.getValue() == m1.getValue() ? 0 : (m2.getValue() > m1.getValue() ? 1 : -1);
                    }
                });

                for (MEvent m : gsetMap.values()) {
                    GsetEvent g = new GsetEvent(m);
                    if (g.getDuration().toHours() > 4) {
                        g.setInUse(beamPresentRfOnMap.getTrueDuration(m.getStart(), m.getEnd()));
                        if (g.getInUse().toHours() > 4) {
                            int numTrips = tripMap.getNumTrips(cavName, m.getStart(), m.getEnd());
                            g.setTripRate(numTrips / (0d + g.getInUse().toMinutes() / 60d));
                            if (g.getTripRate() < 0.5) {
                                topGsets.add(g);
                            }
                        }
                    }
                }

                int i = 0;
                for (GsetEvent g : topGsets) {

                    // Add the portion of the CSV line for the GsetEvent
                    String line = g.toCSVString();

                    // Add all of the metadata to the line as well
                    for (String pv : gsetMetadataPvs) {
                        System.out.print("    " + pv + " -- " + LocalDateTime.now());
                        Event e = getPoint(pService, pv, g.getStart());
                        System.out.println(" -- " + LocalDateTime.now());

                        String time = null;
                        String value = null;
                        if (e != null) {
                            String[] eString = e.toString().split(" ");
                            time = eString[0] + "T" + eString[1];
                            value = eString[2];
                        }
                        line = line + "," + time + "," + value;
                    }
                    csvContent.add(line);
                    // Only want the top ten
                    if (++i >= 10) {
                        break;
                    }
                }
            }

            Charset charset = Charset.forName("UTF-8");
            Path path = Paths.get(System.getProperty("user.home"), "MaxGSET.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(path, charset)) {
                for (String line : csvContent) {
                    writer.write(line + "\n");
                }
            }
        }
    }
}
