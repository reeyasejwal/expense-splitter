package com.es.algo;

import java.util.List;

public class Anomaly {

    /**
     * Z-score based anomaly detection.
     * Z = (value - mean) / stdDev
     * Z > 2.5 means outside 99% of normal distribution — statistically unusual.
     * Returns a warning string if anomaly found, null if expense looks normal.
     */
    public static String check(double amount, List<Double> history) {
        if (history.size() < 3) return null;

        double mean = history.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = history.stream()
            .mapToDouble(d -> Math.pow(d - mean, 2))
            .average().orElse(0);
        double std = Math.sqrt(variance);

        if (std < 1.0)  std  = mean * 0.1;
        if (std < 0.01) return null;

        double z = (amount - mean) / std;

        if (z > 2.5) {
            return String.format(
                "Rs.%.0f is %.1f std deviations above avg (Rs.%.0f). Unusual!",
                amount, z, mean
            );
        }
        if (amount > mean * 5) {
            return String.format(
                "Rs.%.0f is 5x the group average (Rs.%.0f). Please verify.",
                amount, mean
            );
        }
        return null;
    }
}
