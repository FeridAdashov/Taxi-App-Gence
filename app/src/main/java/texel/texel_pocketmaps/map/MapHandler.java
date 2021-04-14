package texel.texel_pocketmaps.map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.util.Parameters.Algorithms;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;

import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import texel.ChangedOSMLibraries.LabelLayer;
import texel.android.GHAsyncTask;
import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Tariff;
import texel.texel_pocketmaps.DataClasses.TaxiInfo;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.Services.TaxiForegroundService;
import texel.texel_pocketmaps.activities.MapActivity;
import texel.texel_pocketmaps.activities.ShowLocationActivity;
import texel.texel_pocketmaps.model.listeners.MapHandlerListener;
import texel.texel_pocketmaps.navigator.NaviEngine;
import texel.texel_pocketmaps.util.TargetDirComputer;
import texel.texel_pocketmaps.util.Variable;


public class MapHandler {
    protected static MapHandler mapHandler;
    public ItemizedLayer<MarkerItem> itemizedLayer;
    protected volatile boolean prepareInProgress = false;
    protected volatile boolean calcPathActive = false;
    protected GeoPoint startMarker;
    protected GeoPoint endMarker;
    protected boolean needLocation;
    protected MapView mapView;
    protected ItemizedLayer<MarkerItem> customLayer;
    protected PathLayer pathLayer;
    protected PathLayer polylineTrack;
    protected GraphHopper hopper;
    protected MapHandlerListener mapHandlerListener;
    protected String currentArea;
    protected int customIcon = R.drawable.ic_my_location_dark_24dp;
    protected MapFileTileSource tileSource;
    /**
     * need to know if path calculating status change; this will trigger MapActions function
     */
    protected boolean needPathCal;
    MapPosition tmpPos = new MapPosition();
    File mapsFolder;
    FloatingActionButton naviCenterBtn;
    PointList trackingPointList = new PointList();
    private MapActivity mapActivity;

    public MapHandler() {
        setCalculatePath(false, false);
        startMarker = null;
        endMarker = null;
        needLocation = false;
        needPathCal = false;
    }

    public static MapHandler getMapHandler() {
        if (mapHandler == null) {
            reset();
        }
        return mapHandler;
    }

    /**
     * reset class, build a new instance
     */
    public static void reset() {
        mapHandler = new MapHandler();
    }

    /**
     * remove a layer from map layers
     *
     * @param layers
     * @param layer
     */
    public static void removeLayer(Layers layers, Layer layer) {
        if (layers != null && layer != null) {
            layers.remove(layer);
        }
    }

    public void init(MapView mapView, String currentArea, File mapsFolder) {
        this.mapView = mapView;
        this.currentArea = currentArea;
        this.mapsFolder = mapsFolder; // path/to/map/area-gh/
    }

    /**
     * load map to mapView
     *
     * @param areaFolder
     */
    public void loadMap(File areaFolder, MapActivity activity) {
        this.mapActivity = activity;
//        logUser(activity, "xəritə yüklənir");

        // Map events receiver
        mapView.map().layers().add(new MapEventsReceiver(mapView.map()));

        // Map file source
        tileSource = new MapFileTileSource();
        tileSource.setMapFile(new File(areaFolder, currentArea + ".map").getAbsolutePath());

        VectorTileLayer l = mapView.map().setBaseMap(tileSource);

        mapView.map().setTheme(VtmThemes.DEFAULT);
        mapView.map().layers().add(new BuildingLayer(mapView.map(), l));

        //TODO LabelLayer changed here.
        mapView.map().layers().add(new LabelLayer(mapView.map(), l));

        // Markers layer
        itemizedLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
        mapView.map().layers().add(itemizedLayer);
        customLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
        mapView.map().layers().add(customLayer);

        // Map position
        GeoPoint mapCenter = tileSource.getMapInfo().boundingBox.getCenterPoint();
        mapView.map().setMapPosition(mapCenter.getLatitude(), mapCenter.getLongitude(), 1 << 12);

        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        activity.addContentView(mapView, params);

        loadGraphStorage(activity);
    }

    void loadGraphStorage(final MapActivity activity) {
//        logUser(activity, "loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                // Why is "shortest" missing in default config? Add!
                tmpHopp.getCHFactoryDecorator().addCHProfileAsString("shortest");
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser(activity, "Qrafik yaradılarkən xəta baş verdi:" + getErrorMessage());
                } else {
//                    logUser(activity, "Qrafik yükləndi.");
                }

                GeoPoint g = ShowLocationActivity.locationGeoPoint;
                String lss = ShowLocationActivity.locationSearchString;
                if (g != null) {
                    activity.getMapActions().onPressLocationEndPoint(g);
                    ShowLocationActivity.locationGeoPoint = null;
                } else if (lss != null) {
                    activity.getMapActions().startGeocodeActivity(null, null, false, false);
                }
                prepareInProgress = false;
            }
        }.execute();
    }

    public AllEdgesIterator getAllEdges() {
        if (hopper == null) {
            return null;
        }
        if (hopper.getGraphHopperStorage() == null) {
            return null;
        }
        return hopper.getGraphHopperStorage().getAllEdges();
    }

    /**
     * center the LatLong point in the map and zoom map to zoomLevel
     *
     * @param latLong
     * @param zoomLevel (if 0 use current zoomlevel)
     */
    public void centerPointOnMap(GeoPoint latLong, int zoomLevel, float bearing, float tilt) {
        if (zoomLevel == 0) {
            zoomLevel = mapView.map().getMapPosition().zoomLevel;
        }
        double scale = 1 << zoomLevel;
        tmpPos.setPosition(latLong);
        tmpPos.setScale(scale);
        tmpPos.setBearing(bearing);
        tmpPos.setTilt(tilt);
        mapView.map().animator().animateTo(300, tmpPos);
    }

    public void resetTilt(float tilt) {
        mapView.map().setMapPosition(mapView.map().getMapPosition().setTilt(tilt));
    }

    /**
     * @return
     */
    public boolean isNeedLocation() {
        return needLocation;
    }

    /**
     * set in need a location from screen point (touch)
     *
     * @param needLocation
     */
    public void setNeedLocation(boolean needLocation) {
        this.needLocation = needLocation;
    }

    /**
     * Set start or end Point-Marker.
     *
     * @param p           The Point to set, or null.
     * @param isStart     True for startpoint false for endpoint.
     * @param recalculate True to calculate path, when booth points are set.
     * @return Whether the path will be recalculated.
     **/
    public boolean setStartEndPoint(Activity activity, GeoPoint p, boolean isStart, boolean recalculate) {
        boolean result = false;
        boolean refreshBoth = false;
        if (startMarker != null && endMarker != null && p != null) {
            refreshBoth = true;
        }

        if (isStart) startMarker = p;
        else endMarker = p;

        if (p != null) {
            centerPointOnMap(p, 16, 0, 0);
            Variable.getVariable().zoomableToCurrentLocation = false;
        }

        // remove routing layers
        if ((startMarker == null || endMarker == null) || refreshBoth) {
            if (pathLayer != null)
                pathLayer.clearPath();
            if (itemizedLayer != null)
                itemizedLayer.removeAllItems();
        }
        if (startMarker != null)
            itemizedLayer.addItem(createMarkerItem(activity, startMarker,
                    R.drawable.ic_location_start_24dp, 0.5f, 1.0f, false, null, null));
        if (endMarker != null) {
            int drawableId = R.drawable.ic_location_red;
            if (Variable.getVariable().setSecondPath)
                drawableId = R.drawable.ic_location_end_24dp;
            itemizedLayer.addItem(createMarkerItem(activity, endMarker, drawableId, 0.5f, 1.0f, false, null, null));
        }
        if (startMarker != null && endMarker != null && recalculate) {
            recalcPath(activity);
            result = true;
        }
        mapView.map().updateMap(true);
        return result;
    }

    public void recalcPath(Activity activity) {
        setCalculatePath(true, true);
        calcPath(startMarker.getLatitude(),
                startMarker.getLongitude(),
                endMarker.getLatitude(),
                endMarker.getLongitude(),
                activity, null, null, null);
    }

    /**
     * Set the custom Point for current location, or null to delete.
     * Sets the offset to center.
     **/
    public void setCustomPoint(Activity activity, GeoPoint p) {
        if (customLayer == null) {
            return;
        } // Not loaded yet.
        customLayer.removeAllItems();
        if (p != null) {
            customLayer.addItem(createMarkerItem(activity, p, customIcon, 0.5f, 0.5f, false, null, null));
            mapView.map().updateMap(true);
        }
    }

    public void setCustomPointIcon(Context appContext, int customIcon) {
        this.customIcon = customIcon;
        if (customLayer.getItemList().size() > 0) { // RefreshIcon
            MarkerItem curSymbol = customLayer.getItemList().get(0);
            MarkerSymbol marker = createMarkerItem(appContext, new GeoPoint(0, 0),
                    customIcon, 0.5f, 0.5f, false, null, null).getMarker();
            curSymbol.setMarker(marker);
        }
    }

    public MarkerItem createMarkerItem(Context appContext, GeoPoint p, int iconResourceId,
                                       float offsetX, float offsetY,
                                       boolean isTaxiIcon, TaxiInfo taxiInfo, String userName) {
        Bitmap bitmap;

        if (isTaxiIcon) {
            String registerId = TextUtils.isEmpty(taxiInfo.registerId) ? "Q/N Yoxdur" : taxiInfo.registerId;
            bitmap = createTaxiIcon(appContext, registerId, taxiInfo.category);
        } else {
            Drawable drawable = ContextCompat.getDrawable(appContext, iconResourceId);
            bitmap = AndroidGraphics.drawableToBitmap(drawable);
            userName = "";
        }

        MarkerSymbol markerSymbol = new MarkerSymbol(bitmap, offsetX, offsetY);
        MarkerItem markerItem = new MarkerItem(userName, "", p);
        markerItem.setMarker(markerSymbol);
        return markerItem;
    }

    public Bitmap createTaxiIcon(Context context, String text, int category) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View inflatedFrame = layoutInflater.inflate(R.layout.icon_layout, null);
        RelativeLayout relativeLayout = inflatedFrame.findViewById(R.id.relative);
        relativeLayout.setDrawingCacheEnabled(true);
        relativeLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        relativeLayout.layout(0, 0, relativeLayout.getMeasuredWidth(), relativeLayout.getMeasuredHeight());
        relativeLayout.buildDrawingCache(true);

        int resourceId = 0;
        switch (category) {
            case 0:
                resourceId = R.drawable.ic_taxi_econom;
                break;
            case 1:
                resourceId = R.drawable.ic_taxi_comfort;
                break;
            case 2:
                resourceId = R.drawable.ic_taxi_curier;
                break;
        }

        TextView textView = relativeLayout.findViewById(R.id.icon_name);
        textView.setText(text);

        ImageView imageView = relativeLayout.findViewById(R.id.icon_button);
        imageView.setImageResource(resourceId);

        android.graphics.Bitmap bitmap = android.graphics.Bitmap
                .createScaledBitmap(relativeLayout.getDrawingCache(), 150, 150, false);

        return new AndroidBitmap(bitmap);
    }

    /**
     * @return true if already loaded
     */
    boolean isReady() {
        return !prepareInProgress;
    }

    /**
     * start tracking : reset polylineTrack & trackingPointList & remove polylineTrack if exist
     */
    public void startTrack(Activity activity) {
        if (polylineTrack != null) {
            removeLayer(mapView.map().layers(), polylineTrack);
        }
        polylineTrack = null;
        trackingPointList.clear();
        if (polylineTrack != null) {
            polylineTrack.clearPath();
        }
        polylineTrack = updatePathLayer(activity, polylineTrack, trackingPointList, 0x99003399, 4);
        NaviEngine.getNaviEngine().startDebugSimulator(activity, true);
    }

    /**
     * add a tracking point
     *
     * @param point
     */
    public void addTrackPoint(Activity activity, GeoPoint point) {
        trackingPointList.add(point.getLatitude(), point.getLongitude());
        updatePathLayer(activity, polylineTrack, trackingPointList, 0x9900cc33, 4);
        mapView.map().updateMap(true);
    }

    private void setCalculatePath(boolean calcPathActive, boolean callListener) {
        this.calcPathActive = calcPathActive;
        if (mapHandlerListener != null && needPathCal && callListener)
            mapHandlerListener.pathCalculating(calcPathActive);
    }

    public void setNeedPathCal(boolean needPathCal) {
        this.needPathCal = needPathCal;
    }

    /**
     * Get the hopper object, that may be null while loading map.
     *
     * @return GraphHopper object
     */
    public GraphHopper getHopper() {
        return hopper;
    }

    /**
     * assign a new GraphHopper
     *
     * @param hopper
     */
    public void setHopper(GraphHopper hopper) {
        this.hopper = hopper;
    }

    /**
     * only tell on object
     *
     * @param mapHandlerListener
     */
    public void setMapHandlerListener(MapHandlerListener mapHandlerListener) {
        this.mapHandlerListener = mapHandlerListener;
    }

    private void calculateMoneyByDistance(Activity activity, TextView textViewMoney, final double distance) {
        DatabaseFunctions.getDatabases(activity).get(0).child("SETTING/TARIFFS").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double m_distance = distance, money = 0.00;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    if (snap != null)
                        try {
                            Tariff tariff = snap.getValue(Tariff.class);
                            double differ = tariff.getTo() - tariff.getFrom();
                            if (m_distance > differ) {
                                money += (differ / tariff.getEvery()) * tariff.getMoney();
                                m_distance -= differ;
                            } else {
                                money += (m_distance / tariff.getEvery()) * tariff.getMoney();
                                break;
                            }

                        } catch (Exception e) {
                            Log.d("AAAAAAA", "ERROR WHILE CALCULATING MONEY:  " + e.toString());
                        }
                }
                textViewMoney.setText(String.format("%.2f", money) + " AZN");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void calcPath(final double fromLat, final double fromLon,
                         final double toLat, final double toLon, final Activity activity,
                         final TextView textViewDistance,
                         final TextView textViewTime,
                         final TextView textViewMoney) {

        setCalculatePath(true, false);
        log("calculating path ...");
        new AsyncTask<Void, Void, GHResponse>() {
            float time;

            @Override
            protected GHResponse doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).setAlgorithm(Algorithms.DIJKSTRA_BI);
                req.getHints().put(Routing.INSTRUCTIONS, Variable.getVariable().getDirectionsON());
                req.setVehicle("car");
                req.setWeighting(Variable.getVariable().getWeighting());
                if (Variable.getVariable().isShowingSpeedLimits()) {
                    req.getPathDetails().add(com.graphhopper.routing.profiles.MaxSpeed.KEY);
                    req.getPathDetails().add(com.graphhopper.util.Parameters.Details.AVERAGE_SPEED);
                }
                GHResponse resp = null;
                if (hopper != null) {
                    resp = hopper.route(req);
                }
                if (resp == null || resp.hasErrors()) {
                    NaviEngine.getNaviEngine().setDirectTargetDir(true);
                    Throwable error;
                    if (resp != null) {
                        error = resp.getErrors().get(0);
                    } else {
                        error = new NullPointerException("Hopper is null!!!");
                    }
                    log("Multible errors, first: " + error);
                    resp = TargetDirComputer.getInstance().createTargetdirResponse(fromLat, fromLon, toLat, toLon);
                } else {
                    NaviEngine.getNaviEngine().setDirectTargetDir(false);
                }
                time = sw.stop().getSeconds();
                return resp;
            }

            @Override
            protected void onPostExecute(GHResponse ghResp) {
                if (!ghResp.hasErrors()) {
                    PathWrapper resp = ghResp.getBest();

                    float distance = (int) (resp.getDistance() / 100) / 10f;

                    if (textViewDistance != null || textViewMoney != null || textViewTime != null) {
                        if (textViewDistance != null) textViewDistance.setText(distance + " km");
                        if (textViewTime != null)
                            textViewTime.setText(String.format("%.2f", resp.getTime() / 60000f) + " min.");
                        if (textViewMoney != null)
                            calculateMoneyByDistance(activity, textViewMoney, resp.getDistance());
                        return;
                    }

                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
//                    logUser(activity, "the route is " + distance + "km long, time:" + resp.getTime() / 60000f + "min.");

                    if ((!Variable.getVariable().setFirstPath && !Variable.getVariable().setSecondPath)
                            || Variable.getVariable().createNavigation) {
                        int sWidth = 4;
                        pathLayer = updatePathLayer(activity, pathLayer, resp.getPoints(), 0x9900cc33, sWidth);
                        mapView.map().updateMap(true);
                    }
                    if (Variable.getVariable().isDirectionsON()) {
                        Navigator.getNavigator().setGhResponse(resp);
                    }
                } else {
                    logUser(activity, "Səhv baş verdi: " + ghResp.getErrors().size());
                    log("Multible errors, first: " + ghResp.getErrors().get(0));
                }
                setCalculatePath(false, !NaviEngine.getNaviEngine().isNavigating());
                try {
                    if (Variable.getVariable().createNavigation) {
                        Navigator.getNavigator().setNaviStart(activity, true);
                        Location curLoc = MapActivity.getmCurrentLocation();
                        if (curLoc != null) {
                            NaviEngine.getNaviEngine().updatePosition(activity, curLoc);
                            Navigator.getNavigator().setOn(true);
                        }
                    }
                    if (!TaxiForegroundService.isServiceActive) {
                        activity.findViewById(R.id.nav_settings_layout).setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private PathLayer updatePathLayer(Activity activity, PathLayer ref, PointList pointList, int color, int strokeWidth) {
        if (ref == null) {
            ref = createPathLayer(activity, color, strokeWidth);
            mapView.map().layers().add(ref);
        }
        List<GeoPoint> geoPoints = new ArrayList<>();
        //TODO: Search for a more efficient way
        for (int i = 0; i < pointList.getSize(); i++)
            geoPoints.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
        ref.setPoints(geoPoints);
        return ref;
    }

    public void joinPathLayerToPos(double lat, double lon) {
        try {
            List<GeoPoint> geoPoints = new ArrayList<>();
            geoPoints.add(new GeoPoint(lat, lon));
            geoPoints.add(pathLayer.getPoints().get(1));
            pathLayer.setPoints(geoPoints);
        } catch (Exception e) {
            log("Error: " + e);
        }
    }

    private PathLayer createPathLayer(Activity activity, int color, int strokeWidth) {
        Style style = Style.builder()
                .fixed(true)
                .generalization(Style.GENERALIZATION_SMALL)
                .strokeColor(color)
                .strokeWidth(strokeWidth * activity.getResources().getDisplayMetrics().density)
                .build();
        return new PathLayer(mapView.map(), style);
    }

    public void showNaviCenterBtn(boolean visible) {
        if (visible) {
            naviCenterBtn.setVisibility(View.VISIBLE);
        } else {
            naviCenterBtn.setVisibility(View.INVISIBLE);
        }
    }

    public void setNaviCenterBtn(final FloatingActionButton naviCenterBtn) {
        this.naviCenterBtn = naviCenterBtn;
    }

    private void logUser(Activity activity, String str) {
        log(str);
        try {
            Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(String str) {
        Log.i(MapHandler.class.getName(), str);
    }

    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(org.oscim.map.Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {
            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                if (mapHandlerListener != null && needLocation) {
                    mapHandlerListener.onPressLocation(p);
                }
            }
            return false;
        }
    }
}

