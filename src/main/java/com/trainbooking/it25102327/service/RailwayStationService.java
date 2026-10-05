package com.trainbooking.it25102327.service;

import com.trainbooking.it25102327.dto.RouteDetailDto;
import com.trainbooking.it25102327.dto.StationDto;
import com.trainbooking.it25102327.model.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service providing railway network station geospatial coordinates,
 * intermediate station sequences, track geometry, and route corridor lookups.
 * Includes automated railway track distance calculations and high-resolution track alignments.
 *
 * @author SLIIT Software Engineering Team (IT25102327)
 * @version 1.0.0
 */
@Service
@Slf4j
public class RailwayStationService {

    // Master list of all registered Sri Lankan railway stations
    private static final Map<String, StationDto> STATION_REGISTRY = new LinkedHashMap<>();

    // Ordered station corridors
    private static final List<StationDto> MAIN_LINE = new ArrayList<>();
    private static final List<StationDto> COASTAL_LINE = new ArrayList<>();
    private static final List<StationDto> NORTHERN_LINE = new ArrayList<>();
    private static final List<StationDto> EASTERN_LINE = new ArrayList<>();

    // High-resolution physical railway track geometry coordinates [lat, lng]
    private static final List<List<Double>> MAIN_LINE_TRACK = new ArrayList<>();
    private static final List<List<Double>> COASTAL_LINE_TRACK = new ArrayList<>();
    private static final List<List<Double>> NORTHERN_LINE_TRACK = new ArrayList<>();

    static {
        // --- 1. Main Line (Colombo Fort -> Badulla) Stations ---
        addStationToLine(MAIN_LINE, "Colombo Fort", "FOT", 6.9344, 79.8503, 0, 10, "Main Line");
        addStationToLine(MAIN_LINE, "Maradana", "MDA", 6.9298, 79.8654, 2, 7, "Main Line");
        addStationToLine(MAIN_LINE, "Ragama", "RGM", 7.0278, 79.9238, 14, 4, "Main Line");
        addStationToLine(MAIN_LINE, "Gampaha", "GPH", 7.0911, 79.9998, 26, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Veyangoda", "VGD", 7.1472, 80.0594, 37, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Mirigama", "MIR", 7.2428, 80.1292, 49, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Polgahawela", "PLG", 7.3325, 80.2975, 73, 4, "Main Line");
        addStationToLine(MAIN_LINE, "Rambukkana", "RBK", 7.3242, 80.3956, 84, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Kadugannawa", "KGW", 7.2561, 80.5217, 106, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Peradeniya", "PDA", 7.2698, 80.5937, 115, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Kandy", "KDY", 7.2906, 80.6337, 121, 4, "Main Line");
        addStationToLine(MAIN_LINE, "Gampola", "GPL", 7.1644, 80.5739, 137, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Nawalapitiya", "NVL", 7.0544, 80.5333, 155, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Hatton", "HTN", 6.8928, 80.5956, 175, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Talawakele", "TWL", 6.9372, 80.6558, 187, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Nanu Oya", "NOA", 6.9536, 80.7483, 206, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Ambewela", "ABL", 6.8833, 80.8000, 222, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Pattipola", "PPL", 6.8550, 80.8286, 224, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Ohiya", "OHI", 6.8167, 80.8500, 230, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Haputale", "HPT", 6.7681, 80.9500, 247, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Diyatalawa", "DLA", 6.8183, 80.9600, 252, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Bandarawela", "BDA", 6.8289, 80.9856, 258, 3, "Main Line");
        addStationToLine(MAIN_LINE, "Ella", "ELL", 6.8667, 81.0467, 271, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Demodara", "DMD", 6.9067, 81.0667, 277, 2, "Main Line");
        addStationToLine(MAIN_LINE, "Badulla", "BAD", 6.9856, 81.0544, 292, 3, "Main Line");

        // --- 2. Coastal Line (Colombo Fort -> Matara -> Beliatta) Stations ---
        addStationToLine(COASTAL_LINE, "Colombo Fort", "FOT", 6.9344, 79.8503, 0, 10, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Kollupitiya", "KLP", 6.9147, 79.8506, 3, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Bambalapitiya", "BPT", 6.8961, 79.8558, 5, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Wellawatte", "WWT", 6.8744, 79.8603, 8, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Dehiwala", "DHW", 6.8517, 79.8647, 11, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Mount Lavinia", "MLV", 6.8350, 79.8631, 14, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Moratuwa", "MRT", 6.7736, 79.8825, 21, 3, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Panadura", "PND", 6.7131, 79.9075, 28, 3, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Wadduwa", "WDA", 6.6667, 79.9333, 34, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Kalutara North", "KTN", 6.5889, 79.9611, 42, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Kalutara South", "KTS", 6.5833, 79.9600, 44, 3, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Beruwala", "BRL", 6.4789, 79.9828, 55, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Aluthgama", "ALT", 6.4319, 79.9989, 61, 3, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Ambalangoda", "ABA", 6.2361, 80.0542, 85, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Hikkaduwa", "HKD", 6.1408, 80.1008, 97, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Galle", "GLE", 6.0367, 80.2170, 116, 4, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Talpe", "TLP", 5.9983, 80.2764, 126, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Koggala", "KOG", 5.9897, 80.3275, 131, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Ahangama", "AHG", 5.9739, 80.3653, 137, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Weligama", "WLG", 5.9744, 80.4286, 144, 2, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Matara", "MTR", 5.9486, 80.5353, 160, 4, "Coastal Line");
        addStationToLine(COASTAL_LINE, "Beliatta", "BLT", 6.0467, 80.6558, 188, 3, "Coastal Line");

        // --- 3. Northern Line Stations ---
        addStationToLine(NORTHERN_LINE, "Colombo Fort", "FOT", 6.9344, 79.8503, 0, 10, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Ragama", "RGM", 7.0278, 79.9238, 14, 4, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Polgahawela", "PLG", 7.3325, 80.2975, 73, 4, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Kurunegala", "KRN", 7.4863, 80.3623, 94, 3, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Maho Junction", "MHO", 7.8167, 80.2500, 136, 4, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Galgamuwa", "GLM", 8.0167, 80.2833, 156, 2, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Anuradhapura", "ANP", 8.3114, 80.4037, 206, 4, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Medawachchiya", "MDW", 8.5408, 80.4939, 237, 3, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Vavuniya", "VAV", 8.7514, 80.4971, 258, 3, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Kilinochchi", "KOC", 9.3803, 80.3992, 332, 3, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Kodikamam", "KDM", 9.6800, 80.2200, 375, 2, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Jaffna", "JFN", 9.6615, 80.0255, 398, 4, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Chunnakam", "CHK", 9.7431, 80.0225, 408, 2, "Northern Line");
        addStationToLine(NORTHERN_LINE, "Kankesanthurai", "KKS", 9.8133, 80.0381, 417, 3, "Northern Line");

        // --- 4. Eastern Line Stations ---
        addStationToLine(EASTERN_LINE, "Colombo Fort", "FOT", 6.9344, 79.8503, 0, 10, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Polgahawela", "PLG", 7.3325, 80.2975, 73, 4, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Maho Junction", "MHO", 7.8167, 80.2500, 136, 4, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Habarana", "HBR", 8.0333, 80.7500, 190, 2, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Gal Oya", "GOA", 8.1500, 80.9000, 215, 3, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Polonnaruwa", "PLN", 7.9333, 81.0000, 240, 3, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Valachchenai", "VLC", 7.9167, 81.5333, 290, 2, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Batticaloa", "BTC", 7.7167, 81.7000, 335, 3, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Kantale", "KTL", 8.3667, 81.0167, 245, 2, "Eastern Line");
        addStationToLine(EASTERN_LINE, "Trincomalee", "TCO", 8.5833, 81.2333, 280, 3, "Eastern Line");

        // --- High-Resolution Physical Railway Track Geometries ---
        populateHighResolutionTrackGeometries();
    }

    private static void populateHighResolutionTrackGeometries() {
        // Main Line Curved Track Coordinates
        double[][] mainTrack = {
                {6.9344, 79.8503}, {6.9320, 79.8580}, {6.9298, 79.8654}, {6.9450, 79.8730},
                {6.9602, 79.8821}, {6.9678, 79.9142}, {6.9854, 79.9189}, {7.0051, 79.9213},
                {7.0278, 79.9238}, {7.0512, 79.9463}, {7.0694, 79.9702}, {7.0781, 79.9798},
                {7.0911, 79.9998}, {7.1124, 80.0210}, {7.1472, 80.0594}, {7.1724, 80.0789},
                {7.1952, 80.0984}, {7.2428, 80.1292}, {7.2641, 80.1589}, {7.2798, 80.1876},
                {7.2889, 80.2089}, {7.3012, 80.2398}, {7.3112, 80.2541}, {7.3204, 80.2741},
                {7.3325, 80.2975}, {7.3341, 80.3421}, {7.3312, 80.3701}, {7.3242, 80.3956},
                {7.2889, 80.4412}, {7.2789, 80.4721}, {7.2654, 80.4901}, {7.2589, 80.5056},
                {7.2561, 80.5217}, {7.2612, 80.5512}, {7.2654, 80.5789}, {7.2698, 80.5937},
                {7.2812, 80.6121}, {7.2906, 80.6337}, {7.2450, 80.5890}, {7.1980, 80.5812},
                {7.1644, 80.5739}, {7.1089, 80.5512}, {7.0544, 80.5333}, {6.9854, 80.5421},
                {6.9554, 80.5401}, {6.9289, 80.5601}, {6.8928, 80.5956}, {6.9212, 80.6212},
                {6.9372, 80.6558}, {6.9589, 80.6876}, {6.9641, 80.7189}, {6.9612, 80.7356},
                {6.9536, 80.7483}, {6.9212, 80.7712}, {6.8833, 80.8000}, {6.8550, 80.8286},
                {6.8167, 80.8500}, {6.7889, 80.8901}, {6.7681, 80.9500}, {6.8183, 80.9600},
                {6.8289, 80.9856}, {6.8456, 81.0189}, {6.8589, 81.0312}, {6.8667, 81.0467},
                {6.8767, 81.0601}, {6.9067, 81.0667}, {6.9354, 81.0612}, {6.9589, 81.0589},
                {6.9856, 81.0544}
        };
        for (double[] pt : mainTrack) {
            MAIN_LINE_TRACK.add(List.of(pt[0], pt[1]));
        }

        // Coastal Line Curved Track Coordinates (hugging the coast)
        double[][] coastalTrack = {
                {6.9344, 79.8503}, {6.9250, 79.8490}, {6.9147, 79.8506}, {6.8961, 79.8558},
                {6.8744, 79.8603}, {6.8517, 79.8647}, {6.8350, 79.8631}, {6.8189, 79.8712},
                {6.7989, 79.8789}, {6.7854, 79.8801}, {6.7736, 79.8825}, {6.7512, 79.8889},
                {6.7321, 79.8978}, {6.7131, 79.9075}, {6.6898, 79.9212}, {6.6667, 79.9333},
                {6.6250, 79.9480}, {6.5989, 79.9578}, {6.5889, 79.9611}, {6.5833, 79.9600},
                {6.5601, 79.9654}, {6.5312, 79.9712}, {6.5054, 79.9789}, {6.4789, 79.9828},
                {6.4554, 79.9901}, {6.4319, 79.9989}, {6.3889, 80.0112}, {6.3654, 80.0212},
                {6.3354, 80.0312}, {6.2812, 80.0412}, {6.2361, 80.0542}, {6.1954, 80.0712},
                {6.1754, 80.0841}, {6.1408, 80.1008}, {6.1212, 80.1154}, {6.1012, 80.1289},
                {6.0612, 80.1789}, {6.0456, 80.2012}, {6.0367, 80.2170}, {6.0212, 80.2456},
                {5.9983, 80.2764}, {5.9921, 80.3012}, {5.9897, 80.3275}, {5.9812, 80.3456},
                {5.9739, 80.3653}, {5.9721, 80.3956}, {5.9744, 80.4286}, {5.9612, 80.4589},
                {5.9512, 80.4789}, {5.9456, 80.5056}, {5.9486, 80.5353}, {5.9554, 80.5612},
                {5.9789, 80.5912}, {6.0012, 80.6154}, {6.0212, 80.6354}, {6.0467, 80.6558}
        };
        for (double[] pt : coastalTrack) {
            COASTAL_LINE_TRACK.add(List.of(pt[0], pt[1]));
        }

        // Northern Line Track Coordinates
        double[][] northernTrack = {
                {6.9344, 79.8503}, {7.0278, 79.9238}, {7.1472, 80.0594}, {7.3325, 80.2975},
                {7.3912, 80.3154}, {7.4412, 80.3412}, {7.4863, 80.3623}, {7.5812, 80.3412},
                {7.6512, 80.3154}, {7.7212, 80.2812}, {7.8167, 80.2500}, {7.9123, 80.2654},
                {8.0167, 80.2833}, {8.1123, 80.3154}, {8.1612, 80.3354}, {8.2212, 80.3654},
                {8.3114, 80.4037}, {8.3612, 80.4354}, {8.4512, 80.4654}, {8.5408, 80.4939},
                {8.6612, 80.4954}, {8.7514, 80.4971}, {8.9012, 80.4812}, {9.0123, 80.4612},
                {9.1212, 80.4412}, {9.2312, 80.4212}, {9.3803, 80.3992}, {9.4512, 80.3956},
                {9.5289, 80.4012}, {9.5812, 80.3212}, {9.6800, 80.2200}, {9.6612, 80.1512},
                {9.6589, 80.0812}, {9.6615, 80.0255}, {9.6912, 80.0241}, {9.7431, 80.0225},
                {9.8133, 80.0381}
        };
        for (double[] pt : northernTrack) {
            NORTHERN_LINE_TRACK.add(List.of(pt[0], pt[1]));
        }
    }

    private static void addStationToLine(List<StationDto> list, String name, String code, double lat, double lng, int dist, int platforms, String line) {
        StationDto dto = StationDto.builder()
                .name(name)
                .code(code)
                .latitude(lat)
                .longitude(lng)
                .distanceKm(dist)
                .platformsCount(platforms)
                .lineName(line)
                .build();
        list.add(dto);
        STATION_REGISTRY.putIfAbsent(name.toLowerCase().trim(), dto);
    }

    /**
     * Resolves the sequential list of intermediate stations along the corridor between origin and destination.
     *
     * @param origin origin station name
     * @param destination destination station name
     * @return ordered list of intermediate and terminal stations
     */
    public List<StationDto> getStationsAlongRoute(String origin, String destination) {
        log.debug("Resolving intermediate stations for route: '{}' ➔ '{}'", origin, destination);
        if (origin == null || destination == null) {
            return Collections.emptyList();
        }

        String origClean = origin.trim();
        String destClean = destination.trim();

        // Search predefined lines
        List<List<StationDto>> candidateLines = List.of(MAIN_LINE, COASTAL_LINE, NORTHERN_LINE, EASTERN_LINE);
        for (List<StationDto> line : candidateLines) {
            int origIdx = findIndex(line, origClean);
            int destIdx = findIndex(line, destClean);

            if (origIdx != -1 && destIdx != -1) {
                return extractSubSequence(line, origIdx, destIdx);
            }
        }

        // Check if origin or destination can be resolved from station registry as a 2-point fallback
        StationDto origDto = findStation(origClean);
        StationDto destDto = findStation(destClean);

        if (origDto != null && destDto != null) {
            List<StationDto> result = new ArrayList<>();
            origDto.setSequence(1);
            origDto.setDistanceKm(0);
            destDto.setSequence(2);
            int dist = calculateTrackDistanceKm(origClean, destClean);
            destDto.setDistanceKm(dist > 0 ? dist : 100);
            result.add(origDto);
            result.add(destDto);
            return result;
        }

        // Generic fallback with default coordinates
        return List.of(
                StationDto.builder().name(origClean).code("ORG").latitude(6.9344).longitude(79.8503).sequence(1).distanceKm(0).platformsCount(2).lineName("Network").build(),
                StationDto.builder().name(destClean).code("DST").latitude(7.2906).longitude(80.6337).sequence(2).distanceKm(100).platformsCount(2).lineName("Network").build()
        );
    }

    /**
     * Autonomously calculates the accurate railway track distance in kilometers between origin and destination.
     * Evaluates official Sri Lanka Railways chainage data, intermediate stops, and geodesic track geometry.
     *
     * @param origin origin station name
     * @param destination destination station name
     * @return track distance in kilometers (rounded integer)
     */
    public int calculateTrackDistanceKm(String origin, String destination) {
        if (origin == null || destination == null) return 100;
        String origClean = origin.trim();
        String destClean = destination.trim();
        if (origClean.equalsIgnoreCase(destClean)) return 0;

        // Check station sequence along known railway corridor
        List<StationDto> stations = getStationsAlongRoute(origClean, destClean);
        if (stations.size() >= 2) {
            StationDto last = stations.get(stations.size() - 1);
            if (last.getDistanceKm() != null && last.getDistanceKm() > 0) {
                return last.getDistanceKm();
            }
        }

        // Geodesic summation across resolved track geometry
        List<List<Double>> trackCoords = getTrackPolylineCoordinates(origClean, destClean);
        if (trackCoords.size() >= 2) {
            double totalKm = 0.0;
            for (int i = 0; i < trackCoords.size() - 1; i++) {
                double lat1 = trackCoords.get(i).get(0);
                double lon1 = trackCoords.get(i).get(1);
                double lat2 = trackCoords.get(i + 1).get(0);
                double lon2 = trackCoords.get(i + 1).get(1);
                totalKm += calculateHaversineKm(lat1, lon1, lat2, lon2);
            }
            return (int) Math.round(totalKm);
        }

        return 100;
    }

    /**
     * Computes the Haversine great-circle distance between two GPS coordinates in kilometers.
     *
     * @param lat1 latitude 1
     * @param lon1 longitude 1
     * @param lat2 latitude 2
     * @param lon2 longitude 2
     * @return distance in kilometers
     */
    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Returns high-resolution curved track geometry coordinates for the given origin and destination.
     * Slices the physical railway alignment coordinates along the corridor.
     *
     * @param origin origin station
     * @param destination destination station
     * @return list of [latitude, longitude] pairs following actual railway curves
     */
    public List<List<Double>> getTrackPolylineCoordinates(String origin, String destination) {
        if (origin == null || destination == null) return Collections.emptyList();
        String origClean = origin.trim();
        String destClean = destination.trim();

        // 1. Check Main Line
        int origMain = findIndex(MAIN_LINE, origClean);
        int destMain = findIndex(MAIN_LINE, destClean);
        if (origMain != -1 && destMain != -1) {
            return sliceTrackGeometry(MAIN_LINE.get(origMain), MAIN_LINE.get(destMain), MAIN_LINE_TRACK);
        }

        // 2. Check Coastal Line
        int origCoast = findIndex(COASTAL_LINE, origClean);
        int destCoast = findIndex(COASTAL_LINE, destClean);
        if (origCoast != -1 && destCoast != -1) {
            return sliceTrackGeometry(COASTAL_LINE.get(origCoast), COASTAL_LINE.get(destCoast), COASTAL_LINE_TRACK);
        }

        // 3. Check Northern Line
        int origNorth = findIndex(NORTHERN_LINE, origClean);
        int destNorth = findIndex(NORTHERN_LINE, destClean);
        if (origNorth != -1 && destNorth != -1) {
            return sliceTrackGeometry(NORTHERN_LINE.get(origNorth), NORTHERN_LINE.get(destNorth), NORTHERN_LINE_TRACK);
        }

        // Fallback: connect station coordinates directly
        List<StationDto> stations = getStationsAlongRoute(origClean, destClean);
        List<List<Double>> coords = new ArrayList<>();
        for (StationDto s : stations) {
            if (s.getLatitude() != null && s.getLongitude() != null) {
                coords.add(List.of(s.getLatitude(), s.getLongitude()));
            }
        }
        return coords;
    }

    private List<List<Double>> sliceTrackGeometry(StationDto startSt, StationDto endSt, List<List<Double>> masterTrack) {
        if (masterTrack.isEmpty()) return Collections.emptyList();

        int startTrackIdx = findClosestTrackIndex(startSt.getLatitude(), startSt.getLongitude(), masterTrack);
        int endTrackIdx = findClosestTrackIndex(endSt.getLatitude(), endSt.getLongitude(), masterTrack);

        List<List<Double>> sliced = new ArrayList<>();
        // Include start station exact coordinate
        sliced.add(List.of(startSt.getLatitude(), startSt.getLongitude()));

        boolean forward = startTrackIdx <= endTrackIdx;
        int step = forward ? 1 : -1;
        for (int i = startTrackIdx; forward ? (i <= endTrackIdx) : (i >= endTrackIdx); i += step) {
            sliced.add(masterTrack.get(i));
        }

        // Include end station exact coordinate
        sliced.add(List.of(endSt.getLatitude(), endSt.getLongitude()));
        return sliced;
    }

    private int findClosestTrackIndex(double lat, double lng, List<List<Double>> track) {
        int bestIdx = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < track.size(); i++) {
            double tLat = track.get(i).get(0);
            double tLng = track.get(i).get(1);
            double dist = calculateHaversineKm(lat, lng, tLat, tLng);
            if (dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    /**
     * Builds full RouteDetailDto including high-precision geometry polyline coordinates and stations.
     * Automatically calculates track distance if missing.
     *
     * @param route Route entity
     * @return RouteDetailDto
     */
    public RouteDetailDto getRouteDetails(Route route) {
        List<StationDto> stations = getStationsAlongRoute(route.getOrigin(), route.getDestination());
        List<List<Double>> coords = getTrackPolylineCoordinates(route.getOrigin(), route.getDestination());

        int calculatedDistance = (route.getDistanceKm() != null && route.getDistanceKm() > 0)
                ? route.getDistanceKm()
                : calculateTrackDistanceKm(route.getOrigin(), route.getDestination());

        String line = !stations.isEmpty() ? stations.get(0).getLineName() : "Sri Lanka Railways Corridor";

        return RouteDetailDto.builder()
                .routeId(route.getId())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .distanceKm(calculatedDistance)
                .defaultPlatform(route.getDefaultPlatform() != null ? route.getDefaultPlatform() : "Platform 1")
                .lineName(line)
                .stations(stations)
                .polylineCoords(coords)
                .build();
    }

    /**
     * Retrieves all registered railway stations.
     *
     * @return list of StationDto
     */
    public List<StationDto> getAllStations() {
        return new ArrayList<>(STATION_REGISTRY.values());
    }

    /**
     * Retrieves a single station by name.
     *
     * @param name station name
     * @return StationDto or null
     */
    public StationDto findStation(String name) {
        if (name == null) return null;
        return STATION_REGISTRY.get(name.toLowerCase().trim());
    }

    private int findIndex(List<StationDto> line, String stationName) {
        for (int i = 0; i < line.size(); i++) {
            if (line.get(i).getName().equalsIgnoreCase(stationName)) {
                return i;
            }
        }
        return -1;
    }

    private List<StationDto> extractSubSequence(List<StationDto> line, int startIdx, int endIdx) {
        List<StationDto> sub = new ArrayList<>();
        boolean forward = startIdx <= endIdx;
        int step = forward ? 1 : -1;
        int seq = 1;
        int baseDist = line.get(startIdx).getDistanceKm();

        for (int i = startIdx; forward ? (i <= endIdx) : (i >= endIdx); i += step) {
            StationDto ref = line.get(i);
            int relDist = Math.abs(ref.getDistanceKm() - baseDist);
            StationDto copy = StationDto.builder()
                    .name(ref.getName())
                    .code(ref.getCode())
                    .latitude(ref.getLatitude())
                    .longitude(ref.getLongitude())
                    .sequence(seq++)
                    .distanceKm(relDist)
                    .platformsCount(ref.getPlatformsCount())
                    .lineName(ref.getLineName())
                    .build();
            sub.add(copy);
        }
        return sub;
    }
}
