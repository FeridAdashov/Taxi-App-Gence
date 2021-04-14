package texel.texel_pocketmaps.activities;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oscim.core.GeoPoint;

import java.io.File;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.map.Destination;
import texel.texel_pocketmaps.map.MapHandlerPassenger;
import texel.texel_pocketmaps.map.Navigator;
import texel.texel_pocketmaps.map.Tracking;
import texel.texel_pocketmaps.navigator.NaviEngine;
import texel.texel_pocketmaps.util.SetStatusBarColor;
import texel.texel_pocketmaps.util.Variable;

/**
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on July 04, 2015.
 */
public class MapActivityPassenger extends MapActivity implements LocationListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_passenger);

        MapHandlerPassenger.getMapHandler().init(mapView, Variable.getVariable().getCountry(), Variable.getVariable().getMapsFolder());
        try {
            MapHandlerPassenger.getMapHandler().loadMap(new File(Variable.getVariable().getMapsFolder().getAbsolutePath(),
                    Variable.getVariable().getCountry() + "-gh"), this);
            getIntent().putExtra("texel.pocketmaps.activities.PocketMaps.MapActivityPassenger.SELECTNEWMAP", false);
        } catch (Exception e) {
            logUser("Xəritə fayllarında səhvlik var!\nXəritəni yenidən yükləyin.");
            log("Error while loading map!");
            e.printStackTrace();
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("texel.pocketmaps.activities.PocketMaps.MapActivityPassenger.SELECTNEWMAP", true);
            startActivity(intent);
            return;
        }
        customMapView();
        checkGpsAvailability();
        ensureLastLocationInit();
        updateCurrentLocation(null);
        mapAlive = true;
    }

    public void ensureLocationListener(boolean showMsgEverytime) {
        if (!Variable.getVariable().zoomableToCurrentLocation) return;
        ensureLocationListenerHelper(showMsgEverytime);
        try {
            MapHandlerPassenger.getMapHandler().centerPointOnMap(
                    new GeoPoint(MapActivityPassenger.getmCurrentLocation().getLatitude(),
                            MapActivityPassenger.getmCurrentLocation().getLongitude()), 15, 0, 0);
        } catch (Exception e) {
            Log.d("AAAAAAA", e.toString());
        }
    }

    /**
     * inject and inflate activity map content to map activity context and bring it to front
     */
    private void customMapView() {
        customMapViewHelper();
        mapActions = new MapActionsPassenger(this, mapView);
    }

    protected void customMapViewHelper() {
        ViewGroup inclusionViewGroup = findViewById(R.id.custom_map_view_layout);
        View inflate = LayoutInflater.from(this).inflate(R.layout.activity_map_content, null);
        inclusionViewGroup.addView(inflate);

        inclusionViewGroup.getParent().bringChildToFront(inclusionViewGroup);
        new SetStatusBarColor().setSystemBarColor(findViewById(R.id.statusBarBackgroundMap),
                getResources().getColor(R.color.my_primary_dark_transparent), this);
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
                MapHandlerPassenger.getMapHandler().addTrackPoint(this, mcLatLong);
                Tracking.getTracking(getApplicationContext()).addPoint(mCurrentLocation, mapActions.getAppSettings());
            }
            if (NaviEngine.getNaviEngine().isNavigating()) {
                NaviEngine.getNaviEngine().updatePosition(this, mCurrentLocation);
            }
            MapHandlerPassenger.getMapHandler().setCustomPoint(this, mcLatLong);
            mapActions.showPositionBtn.setImageResource(R.drawable.ic_my_location_white_24dp);
        } else {
            mapActions.showPositionBtn.setImageResource(R.drawable.ic_location_searching_white_24dp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        ensureLocationListener(true);
        ensureLastLocationInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapAlive = false;
        locationManager.removeUpdates(this);
        kalmanLocationManager.removeUpdates(this);
        lastProvider = null;
        mapView.onDestroy();
        if (MapHandlerPassenger.getMapHandler().getHopper() != null)
            MapHandlerPassenger.getMapHandler().getHopper().close();
        MapHandlerPassenger.getMapHandler().setHopper(null);
        Navigator.getNavigator().setOn(false);
        MapHandlerPassenger.reset();
        Destination.getDestination().setStartPoint(null, null);
        Destination.getDestination().setEndPoint(null, null);
        System.gc();
    }

    @Override
    public void onBackPressed() {
        boolean back = ((MapActionsPassenger) mapActions).homeBackKeyPressed();
        if (back) {
            moveTaskToBack(true);
        }
        // if false do nothing
    }
}
