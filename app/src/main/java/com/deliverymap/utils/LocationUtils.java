package com.deliverymap.utils;

/**
 * Utility per calcoli geografici.
 */
public class LocationUtils {

    /**
     * Calcola la distanza in km tra due coordinate usando la formula di Haversine.
     * @param lat1 Latitudine punto 1
     * @param lon1 Longitudine punto 1
     * @param lat2 Latitudine punto 2
     * @param lon2 Longitudine punto 2
     * @return Distanza in chilometri
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // raggio della Terra in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Formatta la distanza in modo leggibile (es. "250 m" oppure "1.4 km").
     */
    public static String formatDistanza(double km) {
        if (km < 1.0) {
            return String.format("%d m", (int) (km * 1000));
        } else {
            return String.format("%.1f km", km);
        }
    }
}
