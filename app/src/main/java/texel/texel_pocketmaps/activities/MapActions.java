package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.oscim.core.GeoPoint;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.fragments.AppSettings;
import texel.texel_pocketmaps.map.MapHandler;
import texel.texel_pocketmaps.model.listeners.MapHandlerListener;
import texel.texel_pocketmaps.model.listeners.NavigatorListener;
import texel.texel_pocketmaps.util.Variable;

/**
 * This file is part of PocketMaps
 * <p>
 * menu controller, controls menus for map activity
 * <p>
 * Created by GuoJunjun <junjunguo.com> on June 24, 2015.
 */
public class MapActions implements NavigatorListener, MapHandlerListener {

    protected TabAction tabAction = TabAction.None;
    protected Activity activity;
    protected AppSettings appSettings;
    protected FloatingActionButton showPositionBtn;

    public MapActions() {
    }

    public void startGeocodeActivity(GeoPoint[] points, String[] names, boolean isStartP, boolean autoEdit) {
    }

    /**
     * when use press on the screen to get a location form map
     *
     * @param latLong
     */
    @Override
    public void onPressLocation(GeoPoint latLong) {
    }

    protected void onPressLocationHelper(GeoPoint latLong) {
    }

    public void onPressLocationEndPoint(GeoPoint latLong) {
        tabAction = TabAction.EndPoint;
        onPressLocation(latLong);
    }

    @Override
    public void pathCalculating(boolean calculatingPathActive) {
    }

    /**
     * move map to my current location as the center of the screen
     */
    protected void initShowMyLocation() {
        showPositionBtn.setOnClickListener(v -> {
            Variable.getVariable().zoomableToCurrentLocation = true;
            if (MapActivity.getmCurrentLocation() != null) {
                showPositionBtn.setImageResource(R.drawable.ic_my_location_white_24dp);
                MapHandler.getMapHandler().centerPointOnMap(
                        new GeoPoint(MapActivity.getmCurrentLocation().getLatitude(),
                                MapActivity.getmCurrentLocation().getLongitude()), 0, 0, 0);

                //                    mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(
                //                            new LatLong(MapActivity.getmCurrentLocation().getLatitude(),
                //                                    MapActivity.getmCurrentLocation().getLongitude()),
                //                            mapView.getModel().mapViewPosition.getZoomLevel()));

            } else {
                showPositionBtn.setImageResource(R.drawable.ic_location_searching_white_24dp);
                Toast.makeText(activity, "Məkan aktiv deyil", Toast.LENGTH_SHORT).show();
            }
            ((MapActivity) activity).ensureLocationListener(false);
        });
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    /**
     * the change on navigator: navigation is used or not
     *
     * @param on
     */
    @Override
    public void onStatusChanged(boolean on) {
    }

    @Override
    public void onNaviStart(boolean on) {
    }

    enum TabAction {StartPoint, EndPoint, AddFavourit, None}
}