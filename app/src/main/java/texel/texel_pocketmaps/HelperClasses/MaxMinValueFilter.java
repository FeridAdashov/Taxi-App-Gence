package texel.texel_pocketmaps.HelperClasses;

import android.text.InputFilter;
import android.text.Spanned;

public class MaxMinValueFilter implements InputFilter {
    private final double mIntMin;
    private final double mIntMax;

    public MaxMinValueFilter(double minValue, double maxValue) {
        this.mIntMin = minValue;
        this.mIntMax = maxValue;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            double input = Double.parseDouble(dest.toString() + source.toString());
            if (isInRange(mIntMin, mIntMax, input)) return null;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isInRange(double a, double b, double c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}