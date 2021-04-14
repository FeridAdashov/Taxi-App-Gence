package texel.texel_pocketmaps.fragments;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.activities.MainActivity;
import texel.texel_pocketmaps.activities.MapActivity;
import texel.texel_pocketmaps.map.Tracking;
import texel.texel_pocketmaps.util.Variable;

/**
 * This file is part of PocketMaps
 * <p>
 * Created by GuoJunjun <junjunguo.com> on July 01, 2015.
 */
public class AppSettings {
    private final Activity activity;
    private final ViewGroup appSettingsVP;
    private final ViewGroup naviLayoutVP;
    private final ViewGroup changeMapItemVP;

    public AppSettings(Activity activity) {
        this.activity = activity;
        appSettingsVP = activity.findViewById(R.id.app_settings_layout);
        naviLayoutVP = activity.findViewById(R.id.app_settings_navigation_layout);
        changeMapItemVP = activity.findViewById(R.id.app_settings_change_map);
    }

    public void showAppSettings(final ViewGroup calledFromVP, SettType settType) {
        initClearBtn(appSettingsVP, calledFromVP);
        if (settType == SettType.Default) {
            changeMapItemVP.setVisibility(View.VISIBLE);
            naviLayoutVP.setVisibility(View.GONE);
            chooseMapBtn(appSettingsVP);
            trackingBtn(appSettingsVP);
        } else {
            naviLayoutVP.setVisibility(View.VISIBLE);
            changeMapItemVP.setVisibility(View.GONE);
            alternateRoute();
            naviDirections();
        }
        appSettingsVP.setVisibility(View.VISIBLE);
        calledFromVP.setVisibility(View.INVISIBLE);
    }

    public ViewGroup getAppSettingsVP() {
        return appSettingsVP;
    }

    private Tracking getTracking() {
        return Tracking.getTracking(activity.getApplicationContext());
    }

    /**
     * init and implement directions checkbox
     */
    private void naviDirections() {
        CheckBox cb = activity.findViewById(R.id.app_settings_directions_cb);
        final CheckBox cb_light = activity.findViewById(R.id.app_settings_light);
        final CheckBox cb_smooth = activity.findViewById(R.id.app_settings_smooth);
        final CheckBox cb_showspeed = activity.findViewById(R.id.app_settings_showspeed);
        final TextView txt_light = activity.findViewById(R.id.txt_light);
        final TextView txt_smooth = activity.findViewById(R.id.txt_smooth);
        final TextView txt_showspeed = activity.findViewById(R.id.txt_showspeed);
        cb.setChecked(Variable.getVariable().isDirectionsON());
        cb_light.setChecked(Variable.getVariable().isLightSensorON());
        cb_smooth.setChecked(Variable.getVariable().isSmoothON());
        cb_showspeed.setChecked(Variable.getVariable().isShowingSpeedLimits());
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Variable.getVariable().setDirectionsON(isChecked);
            cb_light.setEnabled(isChecked);
            cb_smooth.setEnabled(isChecked);
            cb_showspeed.setEnabled(isChecked);
            txt_light.setEnabled(isChecked);
            txt_smooth.setEnabled(isChecked);
            txt_showspeed.setEnabled(isChecked);
        });
        cb_light.setOnCheckedChangeListener((buttonView, isChecked) -> Variable.getVariable().setLightSensorON(isChecked));
        cb_smooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Variable.getVariable().setSmoothON(isChecked);
            ((MapActivity) activity).ensureLocationListener(false);
        });
        cb_showspeed.setOnCheckedChangeListener((buttonView, isChecked) -> Variable.getVariable().setShowSpeedLimits(isChecked));
        if (!Variable.getVariable().isDirectionsON()) {
            cb_light.setEnabled(false);
            cb_smooth.setEnabled(false);
            cb_showspeed.setEnabled(false);
            txt_light.setEnabled(false);
            txt_smooth.setEnabled(false);
            txt_showspeed.setEnabled(false);
        }
    }

    /**
     * init and set alternate route radio button option
     */
    private void alternateRoute() {
        RadioGroup rg = activity.findViewById(R.id.app_settings_weighting_rbtngroup);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            Variable.getVariable().setWeighting("shortest");
//                switch (checkedId) {
//                    case R.id.app_settings_fastest_rbtn:
//                        Variable.getVariable().setWeighting("fastest");
//                        break;
//                    case R.id.app_settings_shortest_rbtn:
//                        Variable.getVariable().setWeighting("shortest");
//                        break;
//                }
        });
        RadioButton rbf, rbs;
        rbf = activity.findViewById(R.id.app_settings_fastest_rbtn);
        rbs = activity.findViewById(R.id.app_settings_shortest_rbtn);
        if (Variable.getVariable().getWeighting().equalsIgnoreCase("shortest")) {//fastest
            rbs.setChecked(true);
        } else {
            rbf.setChecked(true);
        }
    }

    /**
     * tracking item btn handler
     *
     * @param appSettingsVP
     */
    private void trackingBtn(final ViewGroup appSettingsVP) {
        //        final ImageView iv = (ImageView) activity.findViewById(R.id.app_settings_tracking_iv);
        //        final TextView tv = activity.findViewById(R.id.app_settings_tracking_tv);
        trackingBtnClicked();
        final TextView tbtn = activity.findViewById(R.id.app_settings_tracking_tv_switch);
        tbtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tbtn.setBackgroundColor(activity.getResources().getColor(R.color.my_primary_light));
                    return true;
                case MotionEvent.ACTION_UP:
                    tbtn.setBackgroundColor(activity.getResources().getColor(R.color.my_icons));
                    if (getTracking().isTracking()) {
                        confirmWindow();
                    } else {
                        getTracking().startTracking(activity);
                    }
                    trackingBtnClicked();
                    return true;
            }
            return false;
        });
        final TextView tbtnLoad = activity.findViewById(R.id.app_settings_trackload_tv_switch);
        tbtnLoad.setTextColor(activity.getResources().getColor(R.color.my_primary));
        tbtnLoad.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tbtnLoad.setBackgroundColor(activity.getResources().getColor(R.color.my_primary_light));
                    return true;
                case MotionEvent.ACTION_UP:
                    tbtnLoad.setBackgroundColor(activity.getResources().getColor(R.color.my_icons));
                    return true;
            }
            return false;
        });
    }

    private void confirmWindow() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText edittext = new EditText(activity);
        builder.setTitle(activity.getResources().getString(R.string.dialog_stop_save_tracking));
        builder.setMessage("path: " + Variable.getVariable().getTrackingFolder().getAbsolutePath() + "/");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String formattedDate = df.format(System.currentTimeMillis());
        edittext.setText(formattedDate);
        builder.setView(edittext);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        //        builder.setView(inflater.inflate(R.layout.dialog_tracking_exit, null));
        // Add action buttons
        builder.setPositiveButton(R.string.save_data, (dialog, id) -> {
            // save file
            getTracking().saveAsGPX(edittext.getText().toString());
            getTracking().stopTracking();
            trackingBtnClicked();
        }).setNeutralButton(R.string.stop, (dialog, which) -> {
            getTracking().stopTracking();
            trackingBtnClicked();
        }).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        //        ((EditText) ((AlertDialog) dialog).findViewById(R.id.dialog_tracking_exit_et)).setText(formattedDate);
        dialog.show();
    }

    /**
     * dynamic show start or stop tracking
     */
    public void trackingBtnClicked() {
        final ImageView iv = (ImageView) activity.findViewById(R.id.app_settings_tracking_iv);
        final TextView tv = activity.findViewById(R.id.app_settings_tracking_tv_switch);
        if (getTracking().isTracking()) {
            iv.setImageResource(R.drawable.ic_stop_orange_24dp);
            tv.setTextColor(activity.getResources().getColor(R.color.my_accent));
            tv.setText(R.string.tracking_stop);
        } else {
            iv.setImageResource(R.drawable.ic_play_arrow_light_green_a700_24dp);
            tv.setTextColor(activity.getResources().getColor(R.color.my_primary));
            tv.setText(R.string.tracking_start);
            changeMapItemVP.setVisibility(View.VISIBLE);
        }
    }

    /**
     * move to select and load map view
     *
     * @param appSettingsVP
     */
    private void chooseMapBtn(final ViewGroup appSettingsVP) {
        changeMapItemVP.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    changeMapItemVP.setBackgroundColor(activity.getResources().getColor(R.color.my_primary_light));
                    return true;
                case MotionEvent.ACTION_UP:
                    changeMapItemVP.setBackgroundColor(activity.getResources().getColor(R.color.my_icons));
                    // Variable.getVariable().setAutoLoad(false); // close auto load from
                    // main activity
                    MapActivity.isMapAlive_preFinish();
                    activity.finish();
                    startMainActivity();
                    return true;
            }
            return false;
        });
    }

    /**
     * init clear btn
     */
    private void initClearBtn(final ViewGroup appSettingsVP, final ViewGroup calledFromVP) {
        ImageButton appsettingsClearBtn = (ImageButton) activity.findViewById(R.id.app_settings_clear_btn);
        appsettingsClearBtn.setOnClickListener(v -> {
            appSettingsVP.setVisibility(View.INVISIBLE);
            calledFromVP.setVisibility(View.VISIBLE);
        });
    }

    /**
     * move to main activity
     */
    private void startMainActivity() {
        //        if (Tracking.getTracking().isTracking()) {
        //            Toast.makeText(activity, "You need to stop your tracking first!", Toast.LENGTH_LONG).show();
        //        } else {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("texel.pocketmaps.activities.PocketMaps.MapActivity.SELECTNEWMAP", true);
        activity.startActivity(intent);
        //        activity.finish();
        //        }
    }

    /**
     * send message to logcat
     *
     * @param str
     */
    private void log(String str) {
        Log.i(this.getClass().getName(), str);
    }

    /**
     * send message to logcat and Toast it on screen
     *
     * @param str: message
     */
    private void logToast(String str) {
        log(str);
        Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
    }

    public enum SettType {Default}
}
