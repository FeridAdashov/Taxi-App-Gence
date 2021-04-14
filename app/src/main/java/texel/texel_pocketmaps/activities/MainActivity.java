package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FilenameFilter;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.downloader.MapDownloadUnzip;
import texel.texel_pocketmaps.downloader.MapDownloadUnzip.StatusUpdate;
import texel.texel_pocketmaps.map.MapHandler;
import texel.texel_pocketmaps.model.MyMap;
import texel.texel_pocketmaps.model.MyMap.DlStatus;
import texel.texel_pocketmaps.model.MyMap.MapFileType;
import texel.texel_pocketmaps.navigator.NaviText;
import texel.texel_pocketmaps.util.IO;
import texel.texel_pocketmaps.util.Variable;
import texel.texel_pocketmaps.util.Variable.VarType;

/**
 * Shows all local-available maps on a list.
 * <br/>Allows to load a map.
 * <p>
 * <p/>This file is part of PocketMaps
 * <br/>Created by GuoJunjun <junjunguo.com> on July 04, 2015.
 */
public class MainActivity extends AppCompatActivity {
    private boolean changeMap, activityLoaded = false;
    private String adminUsername, adminPassword;

    private Class mapClass = null;

    public static void clearLocalMap(MyMap mm) {
        Variable.getVariable().removeLocalMap(mm);
        File mapsFolder = MyMap.getMapFile(mm, MyMap.MapFileType.MapFolder);
        mm.setStatus(MyMap.DlStatus.On_server);
        int index = Variable.getVariable().getCloudMaps().indexOf(mm);
        if (index >= 0) { // Get same MyMap from CloudList.
            mm = Variable.getVariable().getCloudMaps().get(index);
            mm.setStatus(MyMap.DlStatus.On_server);
        }
        recursiveDelete(mapsFolder);
        log("RecursiveDelete: " + mm.getMapName());
    }

    /**
     * delete a recursively delete a folder or file
     *
     * @param fileOrDirectory
     */
    private static void recursiveDelete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                recursiveDelete(child);
            }
        }
        try {
            fileOrDirectory.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * send message to logcat
     *
     * @param str
     */
    private static void log(String str) {
        Log.i(MainActivity.class.getName(), str);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String profile_name = getIntent().getStringExtra("profile_name");
        if (profile_name == null) finishAffinity();
        else switch (profile_name) {
            case "PASSENGER":
                mapClass = MapActivityPassenger.class;
                break;

            case "ADMIN":
                mapClass = AdminProfileActivity.class;
                break;

            case "TAXI":
                mapClass = MapActivityTaxi.class;
                adminUsername = getIntent().getStringExtra("adminUsername");
                adminPassword = getIntent().getStringExtra("adminPassword");
                break;
        }

        NaviText.initTextList(this);
        continueActivity();
    }

    boolean continueActivity() {
        if (activityLoaded) {
            return true;
        }
        String sPermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (!Permission.checkPermission(sPermission, this)) {
            String sPermission2 = android.Manifest.permission.ACCESS_FINE_LOCATION;
            Permission.startRequest(new String[]{sPermission, sPermission2}, this);
            return false;
        }
        Variable.getVariable().setContext(getApplicationContext());

        boolean loadSuccess = true;
        if (!Variable.getVariable().isLoaded(VarType.Base)) {
            loadSuccess = Variable.getVariable().loadVariables(Variable.VarType.Base);
        }
        if (!loadSuccess) { // First time app started, or loading-error.
            File defMapsDir = IO.getDefaultBaseDirectory(this);
            if (defMapsDir == null) {
                return false;
            }
            Variable.getVariable().setBaseFolder(defMapsDir.getPath());
        }

        if (!Variable.getVariable().getMapsFolder().exists()) {
            Variable.getVariable().getMapsFolder().mkdirs();
        }
        if (!Variable.getVariable().isLoaded(VarType.Geocode)) {
            Variable.getVariable().loadVariables(Variable.VarType.Geocode);
        }

        generateListNow();

        changeMap = getIntent().getBooleanExtra("texel.pocketmaps.activities.PocketMaps.MapActivity.SELECTNEWMAP", true);
        if (Variable.getVariable().getCountry().isEmpty()) {
            changeMap = true;
        }
        // start map activity if load succeed
        if (loadSuccess) {
            if (MapActivity.isMapAlive()) {
                startMapActivity();
            } // Continue map
            else if (!changeMap) {
                startMapActivity();
            }
        }
        activityLoaded = true;
        return true;
    }

    /**
     * read local files and build a list then add the list to mapAdapter
     */
    private void generateListNow() {
        String[] files = Variable.getVariable().getMapsFolder().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return (filename != null && (filename.endsWith("-gh")));
            }
        });
        if (files == null) {
            // Array 'files' was null on a test device.
            log("Warning: mapsFolder does not exist: " + Variable.getVariable().getMapsFolder());
            files = new String[0];
        }

        if (files.length > 0) {
            for (String file : files)
                Variable.getVariable().addLocalMap(new MyMap(file));

            openMap();
        } else startDownloadActivity();
    }

    private void openMap() {
        if (startMapActivityCheck(Variable.getVariable().getLocalMaps().get(0))) {
            startMapActivity();
        }
    }

    /**
     * move to download activity
     */
    private void startDownloadActivity() {
        if (isOnline()) {
            finishAffinity();
            Intent intent = new Intent(this, DownloadMapActivity.class);
            startActivity(intent);
        } else {
            finishAffinity();
            Toast.makeText(this, "İnternetə qoşulun və proqramı yenidən başladın!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean startMapActivityCheck(MyMap myMap) {
        if (MyMap.isVersionCompatible(myMap.getMapName())) {
            Variable.getVariable().setPrepareInProgress(true);
            Variable.getVariable().setCountry(myMap.getMapName());
            if (changeMap) {
                Variable.getVariable().setLastLocation(null);
                MapHandler.reset();
                System.gc();
            }
            return true;
        } else logUser("Proqram versionu yanlışdır!\nProqramı yenidən yükləyin!");
        myMap.checkUpdateAvailableMsg(this);
        return false;
    }

    /**
     * move to map screen
     */
    private void startMapActivity() {
        finishAffinity();
        Intent intent = new Intent(this, mapClass);
        intent.putExtra("adminUsername", adminUsername);
        intent.putExtra("adminPassword", adminPassword);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (continueActivity()) {
            checkMissingMaps();
        }
    }

    private void checkMissingMaps() {
        boolean hasUnfinishedMaps = false;
        File[] fileList = Variable.getVariable().getDownloadsFolder().listFiles();
        if (fileList == null) {
            log("WARNING: Downloads-folder access-error!");
            return;
        }
        for (File file : Variable.getVariable().getDownloadsFolder().listFiles()) {
            if (file.isFile()) {
                if (file.getName().endsWith(".id")) {
                    hasUnfinishedMaps = true;
                    break;
                }
            }
        }
        if (hasUnfinishedMaps) {
            for (MyMap curMap : Variable.getVariable().getCloudMaps()) {
                File idFile = MyMap.getMapFile(curMap, MapFileType.DlIdFile);
                if (idFile.exists()) {
                    MapDownloadUnzip.checkMap(this, curMap, createStatusUpdater());
                }
            }
        }
    }

    private StatusUpdate createStatusUpdater() {
        return new StatusUpdate() {
            @Override
            public void logUserThread(String txt) {
                MainActivity.this.logUserThread(txt);
            }

            @Override
            public void updateMapStatus(MyMap map) {
                MainActivity.this.logUserThread(map.getMapName() + ": " + map.getStatus());
                if (map.getStatus() == DlStatus.Complete) {
                    Variable.getVariable().getLocalMaps().add(map);
                }
            }

            @Override
            public void onRegisterBroadcastReceiver(Activity activity, MyMap myMap, long enqueueId) {
            }
        };
    }

    /**
     * @return true is there is a network connection
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void logUser(String str) {
        Log.i(MainActivity.class.getName(), str);
        try {
            Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logUserThread(final String str) {
        runOnUiThread(() -> logUser(str));
    }
}
