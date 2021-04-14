package texel.texel_pocketmaps.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.oscim.core.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import texel.texel_pocketmaps.geocoding.GeocoderLocal;
import texel.texel_pocketmaps.model.MyMap;

/**
 * variable data might need to be saved to file
 * <p/>
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on June 27, 2015.
 */
public class Variable {
    public static final int DEF_ZOOM = 8;
    public static final String DEF_WIGHTING = "shortest"; // fastest

    //---------------------
    public static final String DEF_ALG = "astarbi";
    private static Variable variable;
    private final HashMap<VarType, Boolean> loadStatus = new HashMap<>();
    /**
     * MY VARIABLES
     */
    public boolean setFirstPath = false, setSecondPath = false, createNavigation = false,
            ensureLocationListenerActive = true, zoomableToCurrentLocation = true;
    public int taxi_active_database_index;
    /**
     * fastest, shortest (route)
     */
    private String weighting;
    /**
     * Bidirectional Dijkstra:      DIJKSTRA_BI             = "dijkstrabi"
     * <p/>
     * Unidirectional Dijkstra:     DIJKSTRA                = "dijkstra"
     * <p/>
     * one to many Dijkstra:        DIJKSTRA_ONE_TO_MANY    = "dijkstraOneToMany"
     * <p/>
     * Unidirectional A* :          ASTAR                   = "astar"
     * <p/>
     * Bidirectional A* :           ASTAR_BI                = "astarbi"
     */
    private String routingAlgorithms;
    /**
     * instructions on or off; default true (on)
     */
    private boolean directionsON;
    /**
     * Show a sign while navigating, that indicates max speed
     */
    private boolean showSpeedLimits;
    /**
     * users current / last used zoom level
     */
    private int lastZoomLevel;
    /**
     * users last browsed screen center location
     */
    private GeoPoint lastLocation;
    /**
     * map directory name: pocketmaps/maps/
     */
    private final String mapDirectory;
    /**
     * download directory name: pocketmaps/downloads/
     */
    private final String dlDirectory;
    /**
     * map directory name: pocketmaps/tracking/
     */
    private final String trackingDirectory;
    /**
     * area or country name (need to be loaded)
     * <p/>
     * example: /storage/emulated/0/Download/(mapDirectory)/(country)-gh
     */
    private String country;
    /**
     * a File where all Areas or counties are in
     * <p/>
     * example:
     * <p/>
     * <li>mapsFolder.getAbsolutePath() = /storage/emulated/0/Download/pocketmaps/maps </li>
     * <p/>
     * <li> mapsFolder   =   new File("/storage/emulated/0/Download/pocketmaps/maps")</li>
     */
    private File mapsFolder;
    /**
     * Server for download JSON list of maps
     */
    private final String mapUrlJSON;
    /**
     * prepare to load the map
     */
    private volatile boolean prepareInProgress;
    /**
     * list of downloaded maps in local storage; check and init when app started; used to avoid recheck local files
     */
    private List<MyMap> localMaps;
    /**
     * temporary memorialize recent downloaded maps from DownloadMapActivity
     */
    private final List<MyMap> recentDownloadedMaps;
    /**
     * temporary memorialize download list of cloud maps from DownloadMapActivity
     */
    private List<MyMap> cloudMaps;
    private int sportCategoryIndex;
    private boolean lightSensorON;
    private boolean smoothON;
    private int offlineSearchBits = GeocoderLocal.BIT_CITY + GeocoderLocal.BIT_STREET;
    private int geocodeSearchEngine = 0;
    private String geocodeSearchTextList = "";
    /**
     * application context
     */
    private Context context;

    private Variable() {
        this.weighting = DEF_WIGHTING;
        this.routingAlgorithms = DEF_ALG;
        this.lastZoomLevel = DEF_ZOOM;
        this.lastLocation = null;
        this.country = null;
        this.mapsFolder = null;
        this.context = null;
        this.directionsON = true;
        this.showSpeedLimits = false;
        this.lightSensorON = true;
        this.smoothON = false;
        this.mapDirectory = "pocketmaps/maps/";
        this.dlDirectory = "pocketmaps/downloads/";
        this.trackingDirectory = "pocketmaps/tracking/";
        this.mapUrlJSON = "http://vsrv15044.customer.xenway.de/maps";
        this.localMaps = new ArrayList<>();
        this.recentDownloadedMaps = new ArrayList<>();
        this.cloudMaps = new ArrayList<>();
        this.sportCategoryIndex = 0;
    }

    public static Variable getVariable() {
        if (variable == null) {
            variable = new Variable();
        }
        return variable;
    }

    public static boolean isSavingFile(String f) {
        return f.startsWith("pocketmapssavedfile") && f.endsWith(".txt");
    }

    public boolean isLoaded(VarType type) {
        return loadStatus.containsKey(type);
    }

    public String getMapUrlJSON() {
        return mapUrlJSON;
    }

    public int getGeocodeSearchEngine() {
        return geocodeSearchEngine;
    }

    public boolean setGeocodeSearchEngine(int geocodeSearchEngine) {
        if (this.geocodeSearchEngine == geocodeSearchEngine) {
            return false;
        }
        this.geocodeSearchEngine = geocodeSearchEngine;
        return true;
    }

    public int getOfflineSearchBits() {
        return offlineSearchBits;
    }

    public void setOfflineSearchBits(int offlineSearchBits) {
        this.offlineSearchBits = offlineSearchBits;
    }

    public String[] getGeocodeSearchTextList() {
        if (geocodeSearchTextList.isEmpty()) {
            return new String[0];
        }
        return geocodeSearchTextList.trim().split("\n");
    }

    public boolean addGeocodeSearchText(String text) {
        if (text.isEmpty()) {
            return false;
        }
        text = text.toLowerCase();
        for (String curTxt : getGeocodeSearchTextList()) {
            if (curTxt.contains(text)) {
                return false;
            }
        }
        geocodeSearchTextList = geocodeSearchTextList + "\n" + text;
        return true;
    }

    public String getWeighting() {
        return weighting;
    }

    public void setWeighting(String weighting) {
        this.weighting = weighting;
    }

    public String getRoutingAlgorithms() {
        return routingAlgorithms;
    }

    public void setRoutingAlgorithms(String routingAlgorithms) {
        this.routingAlgorithms = routingAlgorithms;
    }

    public boolean isDirectionsON() {
        return directionsON;
    }

    public boolean isShowingSpeedLimits() {
        return showSpeedLimits;
    }

    public void setShowSpeedLimits(boolean showSpeedLimits) {
        this.showSpeedLimits = showSpeedLimits;
    }

    public boolean isLightSensorON() {
        return lightSensorON;
    }

    public void setLightSensorON(boolean lightSensorON) {
        this.lightSensorON = lightSensorON;
    }

    public boolean isSmoothON() {
        return smoothON;
    }

    public void setSmoothON(boolean smoothON) {
        this.smoothON = smoothON;
    }

    /**
     * @return is DirectionsON as string : "true or false"
     */
    public String getDirectionsON() {
        return isDirectionsON() ? "true" : "false";
    }

    public void setDirectionsON(boolean directionsON) {
        this.directionsON = directionsON;
    }

    public int getLastZoomLevel() {
        return lastZoomLevel;
    }

    public void setLastZoomLevel(int lastZoomLevel) {
        this.lastZoomLevel = lastZoomLevel;
    }

    public GeoPoint getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(GeoPoint lastLocation) {
        this.lastLocation = lastLocation;
    }

    /**
     * Returns the last selected country, or an empty String.
     **/
    public String getCountry() {
        if (country == null) {
            return "";
        }
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public File getMapsFolder() {
        return mapsFolder;
    }

    public File getDownloadsFolder() {
        File dlFolder = new File(mapsFolder.getParentFile().getParent(), dlDirectory);
        if (!dlFolder.exists()) {
            dlFolder.mkdirs();
        }
        return IO.getDownloadDirectory(dlFolder, context);
    }

    public void setBaseFolder(String baseFolder) {
        this.mapsFolder = new File(baseFolder, mapDirectory);
    }

    public File getTrackingFolder() {
        return new File(mapsFolder.getParentFile().getParent(), trackingDirectory);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean isPrepareInProgress() {
        return prepareInProgress;
    }

    public void setPrepareInProgress(boolean prepareInProgress) {
        this.prepareInProgress = prepareInProgress;
    }

    public List<MyMap> getLocalMaps() {
        return localMaps;
    }

    public void setLocalMaps(List<MyMap> localMaps) {
        this.localMaps = localMaps;
    }

    /**
     * add a list of maps to localMaps
     *
     * @param localMaps list of maps
     */
    public void addLocalMaps(List<MyMap> localMaps) {
        this.localMaps.addAll(localMaps);
    }

    /**
     * add a map to local map list
     *
     * @param localMap MyMap
     */
    public void addLocalMap(MyMap localMap) {
        if (!getLocalMapNameList().contains(localMap.getMapName())) {
            this.localMaps.add(localMap);
        }
    }

    public void removeLocalMap(MyMap localMap) {
        this.localMaps.remove(localMap);
    }

    /**
     * @return a string list of local map names (continent_country)
     */
    public List<String> getLocalMapNameList() {
        ArrayList<String> al = new ArrayList<String>();
        for (MyMap mm : getLocalMaps()) {
            al.add(mm.getMapName());
        }
        return al;
    }

    public List<MyMap> getRecentDownloadedMaps() {
        return recentDownloadedMaps;
    }

    public List<MyMap> getCloudMaps() {
        return cloudMaps;
    }

    public void updateCloudMaps(List<MyMap> cloudMaps) {
        ArrayList<MyMap> newList = new ArrayList<MyMap>();
        for (MyMap oldMap : this.cloudMaps) {
            for (MyMap newMap : cloudMaps) {
                if (newMap.getUrl().equals(oldMap.getUrl())) {
                    newMap.setStatus(oldMap.getStatus());
                    break;
                }
            }
        }
        // Find same Map from CloudMaps
        for (MyMap newMap : cloudMaps) {
            int myIndex = Variable.getVariable().getCloudMaps().indexOf(newMap);
            if (myIndex < 0) {
                newList.add(newMap);
                continue;
            }
            MyMap sameMap = Variable.getVariable().getCloudMaps().get(myIndex);
            sameMap.set(newMap);
            newList.add(sameMap);
        }
        this.cloudMaps = newList;
    }

    /**
     * Similar as getTravelMode() but used for spinner.
     **/
    public int getSportCategoryIndex() {
        return sportCategoryIndex;
    }

    /**
     * Similar as setTravelMode() but used for spinner.
     **/
    public void setSportCategoryIndex(int sportCategoryIndex) {
        this.sportCategoryIndex = sportCategoryIndex;
    }

    /**
     * run when app open at run time
     * <p/>
     * load variables from saved file
     *
     * @return true if load succeed, false if nothing to load or load fail
     */
    public boolean loadVariables(VarType varType) {
        String content;
        if (varType == VarType.Base) {
            content = readFile("pocketmapssavedfile.txt");
            loadStatus.put(VarType.Base, Boolean.TRUE);
        } else {
            content = readFile("pocketmapssavedfile_" + varType + ".txt");
            loadStatus.put(VarType.Geocode, Boolean.TRUE);
        }
        if (content == null) {
            return false;
        }
        JsonWrapper jo;
        try {
            jo = new JsonWrapper(content);
            if (varType == VarType.Base) {
                setWeighting(jo.getStr("weighting", DEF_WIGHTING));
                setRoutingAlgorithms(jo.getStr("routingAlgorithms", DEF_ALG));
                setDirectionsON(jo.getBool("directionsON", true));
                setShowSpeedLimits(jo.getBool("showSpeedLimits", false));
                setLightSensorON(jo.getBool("lightSensorON", true));
                setLastZoomLevel(jo.getInt("lastZoomLevel", DEF_ZOOM));
                setSmoothON(jo.getBool("smoothON", false));
                double la = jo.getDouble("latitude", 0);
                double lo = jo.getDouble("longitude", 0);
                if (la != 0 && lo != 0) {
                    setLastLocation(new GeoPoint(la, lo));
                }
                String coun = jo.getStr("country", "");
                if (!coun.isEmpty()) {
                    setCountry(coun);
                }
                String mapsFolderAbsStr = jo.getStr("mapsFolderAbsPath", "/x/y/z");
                File mapsFolderAbsPath = new File(mapsFolderAbsStr);
                if (mapsFolderAbsPath.exists() && !mapsFolderAbsStr.equals("/x/y/z")) {
                    setBaseFolder(mapsFolderAbsPath.getParentFile().getParent());
                }
                setSportCategoryIndex(jo.getInt("sportCategoryIndex", 0));
            } else if (varType == VarType.Geocode) {
//                setGeocodeSearchEngine(jo.getInt("geocodeSearchEngine", 0));
                setGeocodeSearchEngine(0);
                geocodeSearchTextList = jo.getStr("geocodeSearchTextList", "");
                offlineSearchBits = jo.getInt("offlineSearchBits", GeocoderLocal.BIT_CITY + GeocoderLocal.BIT_STREET);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * run before app destroyed at run time
     * <p/>
     * save variables to local file (json)   @return true is succeed, false otherwise
     */
    public boolean saveVariables(VarType varType) {
        JsonWrapper jo = new JsonWrapper();
        try {
            if (varType == VarType.Base) {
                jo.put("weighting", getWeighting());
                jo.put("routingAlgorithms", getRoutingAlgorithms());
                jo.put("directionsON", isDirectionsON());
                jo.put("showSpeedLimits", isShowingSpeedLimits());
                jo.put("lightSensorON", isLightSensorON());
                jo.put("lastZoomLevel", getLastZoomLevel());
                if (getLastLocation() != null) {
                    jo.put("latitude", getLastLocation().getLatitude());
                    jo.put("longitude", getLastLocation().getLongitude());
                } else {
                    jo.put("latitude", 0);
                    jo.put("longitude", 0);
                }
                jo.put("country", getCountry());
                jo.put("smoothON", smoothON);
                jo.put("mapsFolderAbsPath", getMapsFolder().getAbsolutePath());
                jo.put("sportCategoryIndex", getSportCategoryIndex());
            } else if (varType == VarType.Geocode) {
                jo.put("geocodeSearchEngine", geocodeSearchEngine);
                jo.put("geocodeSearchTextList", geocodeSearchTextList);
                jo.put("offlineSearchBits", offlineSearchBits);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (varType == VarType.Base) {
            return saveStringToFile("pocketmapssavedfile.txt", jo.toString());
        } else {
            return saveStringToFile("pocketmapssavedfile_" + varType + ".txt", jo.toString());
        }
    }

    /**
     * Return existing files, where settings are saved.
     **/
    public ArrayList<String> getSavingFiles() {
        ArrayList<String> list = new ArrayList<String>();
        for (String f : context.fileList()) {
            if (isSavingFile(f)) {
                list.add(f);
            }
        }
        return list;
    }

    /**
     * @return read saved file and return it as a string
     */
    private String readFile(String file) {
        try (FileInputStream fis = context.openFileInput(file);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader bufferedReader = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            log("Cant load savingfile, maybe the first time since app installed.");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param file a string need to be saved
     * @return
     */
    private boolean saveStringToFile(String file, String content) {
        try (FileOutputStream fos = context.openFileOutput(file, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void log(String s) {
        Log.i(Variable.class.getName(), s);
    }

    public enum VarType {Base, Geocode}
}
