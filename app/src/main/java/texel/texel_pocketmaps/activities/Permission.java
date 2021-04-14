package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.util.Variable;

public class Permission extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback, OnClickListener {
    static String[] sPermission;
    static int idCounter = 0;
    static boolean isAsking = false;

    public static void startRequest(String[] sPermission, Activity activity) {
        Permission.sPermission = sPermission;

        Intent intent = new Intent(activity, Permission.class);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Check if permission is already permitted.
     *
     * @param sPermission The Permission of android.Manifest.permission.xyz
     **/
    public static boolean checkPermission(String sPermission, Context context) {
        // Check if the Camera permission has been granted
        return ActivityCompat.checkSelfPermission(context, sPermission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        Button okButton = findViewById(R.id.okTextButton);
        TextView listText = findViewById(R.id.areaText);
        listText.setFocusable(false);
        listText.setText(getPermissionText());
        okButton.setOnClickListener(this);
    }

    private CharSequence getPermissionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("İcazələr:\n\n");

        for (String curPermission : sPermission) {
            sb.append(curPermission.replace('.', '\n'));
            sb.append("\n\n");
        }
        return sb;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAsking) {
            if (checkPermission(sPermission[0], this))
                openActivity();
            else
                logUser("Proqram icazələr tələb edir!!!");
        }
        isAsking = false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.okTextButton) {
            log("Selected: Permission-Ok");
            requestPermissionLater(sPermission);
            isAsking = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            openActivity();
        else logUser("Proqramın istifadəsi üçün icazəyə verməyiniz vacibdir!");
    }

    /**
     * Check for permission to permit.
     *
     * @param sPermission The Permission of android.Manifest.permission.xyz
     **/
    private void requestPermissionLater(String[] sPermission) {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                sPermission)) {
        ActivityCompat.requestPermissions(this,
                sPermission,
                getId());
//        } else {
//          logUser("Permission is not available: " + sPermission);
//          return false;
//        }
    }

    private int getId() {
        idCounter++;
        return idCounter;
    }

    private void log(String str) {
        Log.i(Permission.class.getName(), str);
    }

    private void logUser(String str) {
        Log.i(Permission.class.getName(), str);
        try {
            Toast.makeText(this.getBaseContext(), str, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true is there is a network connection
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void startDownloadActivity() {
        if (isOnline()) {
            Intent intent = new Intent(this, DownloadMapActivity.class);
            startActivity(intent);
        } else
            Toast.makeText(this, "İnternetə qoşulun və proqramı yenidən başladın!", Toast.LENGTH_LONG).show();
    }

    private void openActivity() {
        finish();
        if (Variable.getVariable().getLocalMaps().size() > 0)
            startActivity(new Intent(this, MapActivity.class));
        else startDownloadActivity();
    }
}

