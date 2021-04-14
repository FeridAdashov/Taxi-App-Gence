package texel.texel_pocketmaps.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.provider.Settings;

import texel.texel.gencetaxiapp.R;

public class DialogGPSActivate {
    public static void showGpsSelector(final Activity activity) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
        builder1.setTitle(R.string.autoselect_map);
        builder1.setCancelable(true);
        builder1.setTitle(R.string.gps_is_off);
        OnClickListener listener = (dialog, buttonNr) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        };
        builder1.setPositiveButton(R.string.gps_settings, listener);
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
