package org.systemsbiology.genomebrowser.util;

public class CoordinateMapSelection implements Comparable<CoordinateMapSelection> {
    final String name;
    final double percentage;

    public static final CoordinateMapSelection NO_MAPPINGS = new CoordinateMapSelection("-- no mappings found --", 0.0) {
            public String toString() { return name; };
        };

    public CoordinateMapSelection(String name, double percentage) {
        this.name = name;
        this.percentage = percentage;
    }

    @Override
    public String toString() {
        return String.format("%s (%.1f%% match)", name, percentage * 100);
    }

    @Override
    public int compareTo(CoordinateMapSelection other) {
        // null comes first
        if (null == other)
            return -1;

        // natural order is descending numerically
        if (percentage > other.percentage)
            return -1;
        if (percentage < other.percentage)
            return 1;
        return 0;
    }

}