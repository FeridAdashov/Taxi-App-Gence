package texel.texel_pocketmaps.HelperClasses;

import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MyHelperFunctions {
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");

    public static long getDifferentTimeInSeconds(Date orderDateTime) {
        if (orderDateTime == null) return 99999;
        long millis = Math.abs(orderDateTime.getTime() - new Date().getTime());
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    public static String convertSecondsToString(long seconds) {
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        String data = minutes == 0 ? "" : minutes + " dəq ";
        return data + (seconds - TimeUnit.MINUTES.toSeconds(minutes)) + " san";
    }

    public static boolean isBigDifference(String dateString, long maxSeconds) {
        if (TextUtils.isEmpty(dateString)) return true;
        else {
            try {
                Date date = dateFormatter.parse(dateString);
                return MyHelperFunctions.getDifferentTimeInSeconds(date) > maxSeconds;
            } catch (Exception e) {
                Log.d("AAAAA", e.toString());
                return true;
            }
        }
    }

    public static boolean isBigDifference(Date date, long maxSeconds) {
        if (date == null) return true;
        else return MyHelperFunctions.getDifferentTimeInSeconds(date) > maxSeconds;
    }

    public static Date parseStringToDate(String date) {
        try {
            return dateFormatter.parse(date);
        } catch (Exception e) {
            Log.d("AAAAAAAA", e.toString());
            return null;
        }
    }
}
