package texel.texel_pocketmaps.util;

import java.util.Locale;

public class UnitCalculator {
    public static final double METERS_OF_KM = 1000.0;

    public static String getString(double m) {
        if (m < METERS_OF_KM) return Math.round(m) + " metr";
        return (((int) (m / 100)) / 10f) + " km";
    }

    public static String getUnit(boolean big) {
        if (big) {
            return "km";
        }
        return "m";
    }

    /**
     * Returns a rounded Value of KM or MI.
     *
     * @param pdp Post decimal positions.
     **/
    public static String getBigDistance(double m, int pdp) {
        return String.format(Locale.getDefault(), "%." + pdp + "f", m / METERS_OF_KM);
    }

    /**
     * Returns a rounded Value of M or FT.
     **/
    public static String getShortDistance(double m) {
        return "" + Math.round(m);
    }
}
