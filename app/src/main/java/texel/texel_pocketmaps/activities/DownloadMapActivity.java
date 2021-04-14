package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.MyDialogs.DialogAskSomething;
import texel.texel_pocketmaps.downloader.DownloadFiles;
import texel.texel_pocketmaps.downloader.MapDownloadUnzip;
import texel.texel_pocketmaps.downloader.MapDownloadUnzip.StatusUpdate;
import texel.texel_pocketmaps.fragments.MyMapAdapter;
import texel.texel_pocketmaps.model.MyMap;
import texel.texel_pocketmaps.model.MyMap.DlStatus;
import texel.texel_pocketmaps.model.listeners.OnClickMapListener;
import texel.texel_pocketmaps.model.listeners.OnProgressListener;
import texel.texel_pocketmaps.util.IO;
import texel.texel_pocketmaps.util.SetStatusBarColor;
import texel.texel_pocketmaps.util.Variable;

/**
 * Shows all server-side-available maps on a list.
 * Allows to download or update a map.
 * <p>
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on July 04, 2015.
 */
public class DownloadMapActivity extends AppCompatActivity implements OnClickMapListener, SearchView.OnQueryTextListener {
    private static long cloudMapsTime;
    /**
     * download map
     *
     * @param position item position
     */
    private static MyMap myMap;
    private MyMapAdapter myDownloadAdapter;
    private ProgressBar listDownloadPB;
    private TextView listDownloadTV;
    private RecyclerView mapsRV;
    private final ArrayList<BroadcastReceiver> receiverList = new ArrayList<>();

    private final DialogAskSomething dialogAskSomething = new DialogAskSomething(
            "1. Xəritə yüklənənədək proqramı bağlamayın.\n" +
                    "2. Yüklənmə müəyyən vaxt ala bilər!\n" +
                    "3. Daha səmərəli endirmə üçün cihazı WIFI şəbəkəsinə qoşun!\n" +
                    "4. Xəritə yükləndikdən sonra proqramı bağlayın yenidən açın!",
            "Yaxşı", "", "",
            (v) -> DialogAskSomething.alertDialog.dismiss(), null, null, true);


    /**
     * Read all maps data from server.
     *
     * @param mapNameFilter MapName or null for all.
     * @return list of MyMap
     */
    public static List<MyMap> getMapsFromJSsources(String mapNameFilter, OnProgressListener task) {
        ArrayList<MyMap> maps = new ArrayList<>();
        try {
            String jsonDirUrl = Variable.getVariable().getMapUrlJSON();
            String jsonFileUrl = jsonDirUrl + "/map_url-" + MyMap.MAP_VERSION + ".json";
            String jsonContent = DownloadFiles.getDownloader().downloadTextfile(jsonFileUrl);
            task.onProgress(50);
            log("Json fayl yükləndi");
            JSONObject jsonObj = new JSONObject(jsonContent);
            if (jsonObj.has("maps-" + MyMap.MAP_VERSION) && jsonObj.has("maps-" + MyMap.MAP_VERSION + "-path")) {
                String mapsPath = jsonObj.getString("maps-" + MyMap.MAP_VERSION + "-path");
                JSONArray jsonList = jsonObj.getJSONArray("maps-" + MyMap.MAP_VERSION);
                for (int i = 0; i < jsonList.length(); i++) {
                    JSONObject o = jsonList.getJSONObject(i);
                    String name = o.getString("name");
                    if (!mapNameFilter.equals(name)) {
                        continue;
                    }
                    String size = o.getString("size");
                    String time = o.getString("time");
                    MyMap curMap = new MyMap(name, size, time, jsonDirUrl + "/" + mapsPath + "/" + time + "/");
                    if (o.has("newMap")) {
                        curMap.setMapNameNew(o.getString("newMap"));
                    }
                    maps.add(curMap);
                    float progress = i;
                    progress = i / jsonList.length();
                    progress = progress * 50.0f;
                    task.onProgress(50 + (int) progress);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return maps;
    }

    private static BroadcastReceiver createBroadcastReceiver(final Activity activity,
                                                             final StatusUpdate stUpdate,
                                                             final MyMap myMap,
                                                             final long enqueueId) {
        log("Register receiver for map: " + myMap.getMapName());
        BroadcastReceiver receiver = new BroadcastReceiver() {
            boolean isActive = true;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isActive) {
                    return;
                }
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    int dlStatus = MapDownloadUnzip.getDownloadStatus(context, enqueueId);
                    if (dlStatus == DownloadManager.STATUS_SUCCESSFUL) {
                        MapDownloadUnzip.unzipBg(activity, myMap, stUpdate);
                        isActive = false;
//                        setStatus(1);
                    } else if (dlStatus == -1) { // Aborted
                        log("Yükləməni saxla: " + myMap.getMapName());
                        myMap.setStatus(MyMap.DlStatus.On_server);
                        clearDlFile(myMap);
                        isActive = false;
//                        setStatus(0);
                    } else if (dlStatus == DownloadManager.STATUS_FAILED) { // Error
                        log("Xəritə yüklənərkən səhv baş verdi: " + myMap.getMapName());
                        myMap.setStatus(MyMap.DlStatus.Error);
                        clearDlFile(myMap);
                        isActive = false;
//                        setStatus(0);
                    }
                    stUpdate.updateMapStatus(myMap);
                }
            }
        };
        return receiver;
    }

    public static int getStatus() {
        int status = 0;
        if (Environment.getExternalStorageState().equalsIgnoreCase("mounted"))//Check if Device Storage is present
        {
            try {
                File myTxt = MyMap.getMapFile(myMap, MyMap.MapFileType.MyFile);

                FileReader reader = new FileReader(myTxt);
                status = reader.read();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return status;
    }

    public static void setStatus(int status) {
        if (Environment.getExternalStorageState().equalsIgnoreCase("mounted"))//Check if Device Storage is present
        {
            try {
                File myTxt = MyMap.getMapFile(myMap, MyMap.MapFileType.MyFile);

                FileWriter writer = new FileWriter(myTxt);
                writer.write(status); //Writing the text
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearDlFile(MyMap myMap) {
        log("Clearing dl file for map: " + myMap.getMapName());
        File destFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlMapFile);
        if (destFile.exists()) {
            destFile.delete();
        }
        destFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlIdFile);
        if (destFile.exists()) {
            destFile.delete();
        }
    }

    private static void log(String s) {
        Log.i(DownloadMapActivity.class.getName(), s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        // set status bar
        new SetStatusBarColor().setStatusBarColor(findViewById(R.id.statusBarBackgroundDownload), getResources().getColor(R.color.my_primary_dark), this);

        List<MyMap> cloudMaps = Variable.getVariable().getCloudMaps();
        try {
            initializeBaseFolders();

            String[] dlFiles = Variable.getVariable().getDownloadsFolder().list();
            if (cloudMaps.isEmpty() || isCloudMapsUpdateOld()) {
                cloudMaps = null;
            }
            if (cloudMaps != null && dlFiles.length == 0) {
                log("Skip downloading existing cloud-map-list");
                Collections.sort(cloudMaps);
                activateRecyclerView(filterDeprecatedMaps(cloudMaps));
            } else {
                listDownloadPB = findViewById(R.id.my_maps_download_load_list_pb);
                listDownloadTV = findViewById(R.id.my_maps_download_load_list_tv);
                listDownloadTV.bringToFront();
                listDownloadPB.setProgress(0);
                listDownloadPB.setMax(100);
                listDownloadPB.setIndeterminate(false);
                listDownloadPB.setVisibility(View.VISIBLE);
                listDownloadPB.bringToFront();
                activateRecyclerView(new ArrayList<MyMap>());
                downloadList(this, cloudMaps, dlFiles);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeBaseFolders() {
        Variable.getVariable().setContext(getApplicationContext());
        boolean loadSuccess = true;
        if (!Variable.getVariable().isLoaded(Variable.VarType.Base)) {
            loadSuccess = Variable.getVariable().loadVariables(Variable.VarType.Base);
        }
        if (!loadSuccess) { // First time app started, or loading-error.
            File defMapsDir = IO.getDefaultBaseDirectory(this);
            Variable.getVariable().setBaseFolder(defMapsDir.getPath());
        }
        if (!Variable.getVariable().getMapsFolder().exists()) {
            Variable.getVariable().getMapsFolder().mkdirs();
        }
    }

    private boolean isCloudMapsUpdateOld() {
        long now = System.currentTimeMillis();
        long hours = 1000 * 60 * 60 * 3;
        return (cloudMapsTime + hours) <= now;
    }

    /**
     * download and generate a list of countries from server and add them to the list view
     */
    private void downloadList(final Activity activity,
                              final List<MyMap> cloudMaps,
                              final String[] dlFiles) {
        new AsyncTask<URL, Integer, List<MyMap>>() {
            @Override
            protected List<MyMap> doInBackground(URL... params) {
                OnProgressListener procListener = new OnProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        publishProgress(progress);
                    }
                };
                for (String dlFile : dlFiles) {
                    if (dlFile.endsWith(".ghz")) {
                        String tmpMapName = dlFile.substring(0, dlFile.length() - 4);
                        MyMap tmpMap = new MyMap(tmpMapName);
                        for (MyMap curMap : Variable.getVariable().getCloudMaps()) {
                            if (curMap.getMapName().equals(tmpMapName)) {
                                tmpMap = curMap;
                                break;
                            }
                        }
                        StatusUpdate stUpdate = createStatusUpdater();
                        MapDownloadUnzip.checkMap(activity, tmpMap, stUpdate);
                    }
                }
                if (cloudMaps == null) {
                    List<MyMap> myMaps = getMapsFromJSsources("asia_azerbaijan", procListener);
                    Collections.sort(myMaps);
                    return myMaps;
                }
                return cloudMaps;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                listDownloadPB.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(List<MyMap> myMaps) {
                super.onPostExecute(myMaps);
                listReady(myMaps);
                listDownloadPB.setVisibility(View.GONE);
                listDownloadTV.setVisibility(View.GONE);
            }
        }.execute();
    }

    protected StatusUpdate createStatusUpdater() {
        StatusUpdate s = new StatusUpdate() {
            @Override
            public void logUserThread(String txt) {
                DownloadMapActivity.this.logUserThread(txt);
            }

            @Override
            public void updateMapStatus(MyMap map) {
                DownloadMapActivity.this.myDownloadAdapter.refreshMapView(map);
            }

            @Override
            public void onRegisterBroadcastReceiver(Activity activity, MyMap myMap, long enqueueId) {
                BroadcastReceiver br = createBroadcastReceiver(activity, this, myMap, enqueueId);
                activity.registerReceiver(br, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                DownloadMapActivity.this.receiverList.add(br);
            }
        };
        return s;
    }

    /**
     * list of countries are ready
     *
     * @param myMaps MyMap
     */
    private void listReady(List<MyMap> myMaps) {
        if (myMaps.isEmpty()) {
            logUser("Serverlə əlaqə yoxdur!");
        } else {
            Variable.getVariable().updateCloudMaps(myMaps);
            cloudMapsTime = System.currentTimeMillis();
            myDownloadAdapter.clearList();
            myDownloadAdapter.addAll(filterDeprecatedMaps(myMaps));

            downloadMap(0);
            dialogAskSomething.show(this);
        }
    }

    private List<MyMap> filterDeprecatedMaps(List<MyMap> myMaps) {
        ArrayList<MyMap> newList = new ArrayList<MyMap>();
        for (MyMap curMap : myMaps) {
            if (!curMap.getMapNameNew().isEmpty() &&
                    curMap.getStatus() != DlStatus.Complete) {
                continue;
            }
            newList.add(curMap);
        }
        return newList;
    }

    /**
     * active directions, and directions view
     */
    private void activateRecyclerView(List<MyMap> myMaps) {
        mapsRV = findViewById(R.id.my_maps_download_recycler_view);
        // in content do not change the layout size of the RecyclerView
        mapsRV.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mapsRV.setLayoutManager(layoutManager);
        myDownloadAdapter = new MyMapAdapter(myMaps, true);
        mapsRV.setAdapter(myDownloadAdapter);
    }

    private void downloadMap(int position) {
        myMap = myDownloadAdapter.getItem(position);
        if (/*getStatus() == 1 ||*/ myMap.getStatus() == MyMap.DlStatus.Downloading || myMap.getStatus() == MyMap.DlStatus.Unzipping) {
            logUser("Artıq yüklənir!");
            return;
        } else if (myMap.isUpdateAvailable()) {
            MyMap myMapNew = null;
            if (!myMap.getMapNameNew().isEmpty()) {
                int removePos = -1;
                for (int i = 0; i < myDownloadAdapter.getItemCount(); i++) {
                    if (myDownloadAdapter.getItem(i).getMapName().equals(myMap.getMapName())) {
                        removePos = i;
                    }
                    if (myDownloadAdapter.getItem(i).getMapName().equals(myMap.getMapNameNew())) {
                        position = i;
                        myMapNew = myDownloadAdapter.getItem(i);
                    }
                }
                if (removePos < 0 || myMapNew == null) {
                    logUser("Xəritə köhnədir!");
                    return;
                }
                mapsRV.scrollToPosition(position);
                myDownloadAdapter.remove(removePos);
            }
            MainActivity.clearLocalMap(myMap);
            if (myMapNew != null) {
                myMap = myMapNew;
                if (myMap.getStatus() != DlStatus.On_server) {
                    logUser("Yeni xəritə: " + myMap.getMapName());
                    return;
                }
            }
        } else if (myMap.getStatus() == MyMap.DlStatus.Complete) {
            logUser("Artıq yüklənib!");
            return;
        }
        myMap.setStatus(MyMap.DlStatus.Downloading);
        myDownloadAdapter.refreshMapView(myMap);
        String vers = "?v=unknown";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            vers = "?v=" + packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        } // No problem, not important.
        Request request = new Request(Uri.parse(myMap.getUrl() + vers));
        File destFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlMapFile);
        request.setDestinationUri(Uri.fromFile(destFile));
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/pocketmaps");
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long id = dm.enqueue(request);
        File idFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlIdFile);
        IO.writeToFile(id + "", idFile, false);

        StatusUpdate stUpdate = createStatusUpdater();
        BroadcastReceiver br = createBroadcastReceiver(this, stUpdate, myMap, id);
        registerReceiver(br, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        receiverList.add(br);
//        setStatus(1);

//        DownloadMapBroadcastReceiverService.brDownloadReceiver = br;
//        DownloadMapBroadcastReceiverService.enqueueId = id;
//        DownloadMapBroadcastReceiverService.myMap = myMap;
//        DownloadMapBroadcastReceiverService.stUpdate = stUpdate;
//
//        Intent service = new Intent(this, DownloadMapBroadcastReceiverService.class);
//        this.startService(service);
    }

    @Override
    public void onStop() {
        super.onStop();
        Variable.getVariable().saveVariables(Variable.VarType.Base);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (BroadcastReceiver b : receiverList) {
            unregisterReceiver(b);
        }
        receiverList.clear();
    }

    private void logUser(String str) {
        log(str);
        try {
            Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logUserThread(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logUser(str);
            }
        });
    }

    @Override
    public boolean onQueryTextChange(String filterText) {
        myDownloadAdapter.doFilter(filterText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String filterText) {
        return true;
    }

    @Override
    public void onClickMap(int iPos) {
        try {
            // download map
            downloadMap(iPos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}