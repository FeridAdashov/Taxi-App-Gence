package texel.texel_pocketmaps.activities;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;

import java.io.File;

import texel.lib.KalmanLocationManager;
import texel.lib.KalmanLocationManager.UseProvider;
import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.fragments.DialogGPSActivate;
import texel.texel_pocketmaps.map.Destination;
import texel.texel_pocketmaps.map.MapHandler;
import texel.texel_pocketmaps.map.Navigator;
import texel.texel_pocketmaps.map.Tracking;
import texel.texel_pocketmaps.navigator.NaviEngine;
import texel.texel_pocketmaps.util.Variable;

/**
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on July 04, 2015.
 */
public class MapActivity extends AppCompatActivity implements LocationListener {
    /**
     * Request location updates with the highest possible frequency on gps.
     * Typically, this means one update per second for gps.
     */
    protected static final long GPS_TIME = 1000;
    /**
     * For the network provider, which gives locations with less accuracy (less reliable),
     * request updates every 5 seconds.
     */
    protected static final long NET_TIME = 5000;
    /**
     * For the filter-time argument we use a "real" value: the predictions are triggered by a timer.
     * Lets say we want ~25 updates (estimates) per second = update each 40 millis (to make the movement fluent).
     */
    protected static final long FILTER_TIME = 40;
    protected static Location mCurrentLocation;
    protected static boolean mapAlive = false;
    protected MapView mapView;
    protected Location mLastLocation;
    protected MapActions mapActions;
    protected LocationManager locationManager;
    protected KalmanLocationManager kalmanLocationManager;
    protected PermissionStatus locationListenerStatus = PermissionStatus.Unknown;
    protected String lastProvider;

    /**
     * @return my currentLocation
     */
    public static Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    /**
     * Map was startet and until now not stopped!
     **/
    public static boolean isMapAlive() {
        return mapAlive;
    }

    public static void isMapAlive_preFinish() {
        mapAlive = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lastProvider = null;
        setContentView(R.layout.activity_map);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        kalmanLocationManager = new KalmanLocationManager(this);
        kalmanLocationManager.setMaxPredictTime(10000);
        Variable.getVariable().setContext(getApplicationContext());
        mapView = new MapView(this);
        mapView.setClickable(true);
        MapHandler.getMapHandler().init(mapView, Variable.getVariable().getCountry(), Variable.getVariable().getMapsFolder());
        try {
            MapHandler.getMapHandler().loadMap(new File(Variable.getVariable().getMapsFolder().getAbsolutePath(),
                    Variable.getVariable().getCountry() + "-gh"), this);
            getIntent().putExtra("texel.pocketmaps.activities.PocketMaps.MapActivity.SELECTNEWMAP", false);
        } catch (Exception e) {
            logUser("Xəritə fayllarında səhvlik var!\nXahiş olunur, yenidən yükləyin.");
            log("Error while loading map!");
            e.printStackTrace();
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("texel.pocketmaps.activities.PocketMaps.MapActivity.SELECTNEWMAP", true);
            startActivity(intent);
            return;
        }

        checkGpsAvailability();
        ensureLastLocationInit();
        mapAlive = true;
    }

    public void ensureLocationListener(boolean showMsgEverytime) {
        if (!Variable.getVariable().ensureLocationListenerActive) return;

        ensureLocationListenerHelper(showMsgEverytime);
        MapHandler.getMapHandler().centerPointOnMap(
                new GeoPoint(MapActivity.getmCurrentLocation().getLatitude(),
                        MapActivity.getmCurrentLocation().getLongitude()), 15, 0, 0);
    }

    public void ensureLocationListenerHelper(boolean showMsgEverytime) {
        if (locationListenerStatus == PermissionStatus.Disabled) {
            return;
        }
        if (locationListenerStatus != PermissionStatus.Enabled) {
            boolean f_loc = Permission.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, this);
            if (!f_loc) {
                if (locationListenerStatus == PermissionStatus.Requesting) {
                    locationListenerStatus = PermissionStatus.Disabled;
                    return;
                }
                locationListenerStatus = PermissionStatus.Requesting;
                String[] permissions = new String[2];
                permissions[0] = android.Manifest.permission.ACCESS_FINE_LOCATION;
                permissions[1] = android.Manifest.permission.ACCESS_COARSE_LOCATION;
                Permission.startRequest(permissions, this);
                return;
            }
        }
        try {
            if (Variable.getVariable().isSmoothON()) {
                locationManager.removeUpdates(this);
                kalmanLocationManager.requestLocationUpdates(UseProvider.GPS, FILTER_TIME, GPS_TIME, NET_TIME, this, false);
                lastProvider = KalmanLocationManager.KALMAN_PROVIDER;
//                logUser("LocationProvider: " + lastProvider);
            } else {
                kalmanLocationManager.removeUpdates(this);
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                String provider = locationManager.getBestProvider(criteria, true);
                if (provider == null) {
                    lastProvider = null;
                    locationManager.removeUpdates(this);
                    logUser("Məkan bağlıdır!");
                    return;
                } else if (provider.equals(lastProvider)) {
                    if (showMsgEverytime) {
                        logUser("Məkan: " + provider);
                    }
                    return;
                }
                locationManager.removeUpdates(this);
                lastProvider = provider;
                locationManager.requestLocationUpdates(provider, 3000, 5, this);
//                logUser("Məkan: " + provider);
            }
            locationListenerStatus = PermissionStatus.Enabled;
        } catch (SecurityException ex) {
            logUser("Məkan xidmətinə icazə verilməyib!");
        }
    }

    /**
     * check if GPS enabled and if not send user to the GSP settings
     */
    protected void checkGpsAvailability() {
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            DialogGPSActivate.showGpsSelector(this);
        }
    }

    /**
     * Updates the users location based on the location
     *
     * @param location Location
     */
    private void updateCurrentLocation(Location location) {
        if (location != null) {
            mCurrentLocation = location;
        } else if (mLastLocation != null && mCurrentLocation == null) {
            mCurrentLocation = mLastLocation;
        }
        if (mCurrentLocation != null) {
            GeoPoint mcLatLong = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            if (Tracking.getTracking(getApplicationContext()).isTracking()) {
                MapHandler.getMapHandler().addTrackPoint(this, mcLatLong);
                Tracking.getTracking(getApplicationContext()).addPoint(mCurrentLocation, mapActions.getAppSettings());
            }
            if (NaviEngine.getNaviEngine().isNavigating()) {
                NaviEngine.getNaviEngine().updatePosition(this, mCurrentLocation);
            }
            MapHandler.getMapHandler().setCustomPoint(this, mcLatLong);
            mapActions.showPositionBtn.setImageResource(R.drawable.ic_my_location_white_24dp);
        } else {
            mapActions.showPositionBtn.setImageResource(R.drawable.ic_location_searching_white_24dp);
        }
    }

    public MapActions getMapActions() {
        return mapActions;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        ensureLocationListener(true);
        ensureLastLocationInit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove location updates is not needed for tracking
        if (!Tracking.getTracking(getApplicationContext()).isTracking()) {
            locationManager.removeUpdates(this);
            kalmanLocationManager.removeUpdates(this);
            lastProvider = null;
        }
        if (mCurrentLocation != null) {
            GeoPoint geoPoint = mapView.map().getMapPosition().getGeoPoint();
            Variable.getVariable().setLastLocation(geoPoint);
        }
        if (mapView != null)
            Variable.getVariable().setLastZoomLevel(mapView.map().getMapPosition().getZoomLevel());
        Variable.getVariable().saveVariables(Variable.VarType.Base);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapAlive = false;
        locationManager.removeUpdates(this);
        kalmanLocationManager.removeUpdates(this);
        lastProvider = null;
        mapView.onDestroy();
        if (MapHandler.getMapHandler().getHopper() != null)
            MapHandler.getMapHandler().getHopper().close();
        MapHandler.getMapHandler().setHopper(null);
        Navigator.getNavigator().setOn(false);
        MapHandler.reset();
        Destination.getDestination().setStartPoint(null, null);
        Destination.getDestination().setEndPoint(null, null);
        System.gc();
    }

    protected void ensureLastLocationInit() {
        if (mLastLocation != null) {
            return;
        }
        try {
            Location lonet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lonet != null) {
                mLastLocation = lonet;
                return;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            log("NET-Location is not supported: " + e.getMessage());
        }
        try {
            Location logps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (logps != null) {
                mLastLocation = logps;
                return;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            log("GPS-Location is not supported: " + e.getMessage());
        }
    }

    /**
     * Called when the location has changed.
     * <p/>
     * <p> There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
        updateCurrentLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        logUser("Məkan xidməti yandırıldı!");
    }

    @Override
    public void onProviderDisabled(String provider) {
        logUser("Məkan xidməti söndürüldü!");
    }

    /**
     * send message to logcat
     *
     * @param str
     */
    protected void log(String str) {
        Log.i(this.getClass().getName(), str);
    }

    protected void logUser(String str) {
        Log.i(this.getClass().getName(), str);
        try {
            Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum PermissionStatus {Enabled, Disabled, Requesting, Unknown}
}
