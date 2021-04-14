package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.TaxiInfo;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.fragments.AppSettings;
import texel.texel_pocketmaps.map.Destination;
import texel.texel_pocketmaps.map.MapHandler;
import texel.texel_pocketmaps.map.MapHandlerAdmin;
import texel.texel_pocketmaps.map.MapHandlerTaxi;
import texel.texel_pocketmaps.map.Navigator;
import texel.texel_pocketmaps.model.listeners.MapHandlerListener;
import texel.texel_pocketmaps.model.listeners.NavigatorListener;
import texel.texel_pocketmaps.navigator.NaviEngine;


/**
 * This file is part of PocketMaps
 * <p>
 * menu controller, controls menus for map activity
 * <p>
 * Created by GuoJunjun <junjunguo.com> on June 24, 2015.
 */
public class MapActionsAdmin extends MapActions implements NavigatorListener, MapHandlerListener {

    public Timer timerForLocation = new Timer();
    protected FloatingActionButton naviCenterBtn;
    protected ViewGroup sideBarVP, sideBarMenuVP;
    protected boolean menuVisible;
    private final HashMap<String, TaxiInfo> taxiInfoHashMap = new HashMap<>();

    public MapActionsAdmin(Activity activity, MapView mapView) {
        this.activity = activity;
        initViews();

        MapHandlerTaxi.getMapHandler().setMapHandlerListener(this);
        MapHandlerTaxi.getMapHandler().setNaviCenterBtn(naviCenterBtn);

        appSettings = new AppSettings(activity);
        naviCenterBtn.setOnClickListener(createNaviCenterListener());

        initShowMyLocation();

        mapView.map().events.bind(createUpdateListener());
        mapView.map().getEventLayer().enableRotation(true);
        mapView.map().getEventLayer().enableTilt(true);

        MapHandlerAdmin.getMapHandler().itemizedLayer.setOnItemGestureListener(new ItemizedLayer.OnItemGestureListener<MarkerItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, MarkerItem item) {
                showTaxiInfoDialog(item);
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, MarkerItem item) {
                return false;
            }
        });

        timerForLocation.schedule(new TimerTask() {
            @Override
            public void run() {
                settleTaxisOnMap();
            }
        }, 0, 10000);
    }

    private void showTaxiInfoDialog(MarkerItem item) {
        TaxiInfo taxiInfo = taxiInfoHashMap.get(item.title);
        if (taxiInfo == null) return;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.map_taxi_info_alert_view, null);

        String name = TextUtils.isEmpty(taxiInfo.name) ? "Ad Yoxdur" : taxiInfo.name;
        String carNumber = TextUtils.isEmpty(taxiInfo.carNumber) ? "Maşın Təyin Edilməyib" : "Nömrə: " + taxiInfo.carNumber;
        String carColor = TextUtils.isEmpty(taxiInfo.carColor) ? "Rəng Təyin Edilməyib" : "Rəng: " + taxiInfo.carColor;
        String carYear = TextUtils.isEmpty(taxiInfo.carYear) ? "İl Təyin Edilməyib" : "İl: " + taxiInfo.carYear;
        String carModel = TextUtils.isEmpty(taxiInfo.carModel) ? "Model Təyin Edilməyib" : "Model: " + taxiInfo.carModel;
        String phone = TextUtils.isEmpty(taxiInfo.phone) ? "Telefon Təyin Edilməyib" : "Tel: " + taxiInfo.phone;

        ((TextView) dialogView.findViewById(R.id.textViewTaxiName)).setText(name);
        ((TextView) dialogView.findViewById(R.id.textViewCarNumber)).setText(carNumber);
        ((TextView) dialogView.findViewById(R.id.textViewCarColor)).setText(carColor);
        ((TextView) dialogView.findViewById(R.id.textViewCarYear)).setText(carYear);
        ((TextView) dialogView.findViewById(R.id.textViewCarModel)).setText(carModel);
        ((TextView) dialogView.findViewById(R.id.textViewTaxiPhone)).setText(phone);

        ImageView imageViewCall = dialogView.findViewById(R.id.imageViewCallTaxi);
        imageViewCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + taxiInfo.phone));
            activity.startActivity(intent);
        });

        dialogBuilder.setView(dialogView);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    private void settleTaxisOnMap() {
        DatabaseFunctions.getDatabases(activity).get(0).child("USERS/TAXI")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean active;
                        String user, location, dateString;
                        TaxiInfo taxiInfo;

                        if (MapHandlerAdmin.getMapHandler().itemizedLayer != null && MapHandlerAdmin.getMapHandler().itemizedLayer.size() > 0)
                            MapHandlerAdmin.getMapHandler().itemizedLayer.removeAllItems();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            active = dataSnapshot.child("active").getValue(Boolean.class);
                            if (active == null || !active) continue;

                            dateString = dataSnapshot.child("lastActiveTime").getValue(String.class);
                            if (MyHelperFunctions.isBigDifference(dateString, 30)) continue;

                            location = dataSnapshot.child("LOCATION").getKey();
                            if (location == null) continue;

                            user = dataSnapshot.getKey();
                            Integer category = dataSnapshot.child("ABOUT/category").getValue(Integer.class);

                            if (!taxiInfoHashMap.containsKey(user)) {
                                taxiInfo = new TaxiInfo(
                                        dataSnapshot.child("ABOUT/registrationNumber").getValue(String.class),
                                        dataSnapshot.child("ABOUT/name").getValue(String.class),
                                        dataSnapshot.child("ABOUT/carNumber").getValue(String.class),
                                        dataSnapshot.child("ABOUT/carColor").getValue(String.class),
                                        dataSnapshot.child("ABOUT/carYear").getValue(String.class),
                                        dataSnapshot.child("ABOUT/carModel").getValue(String.class),
                                        dataSnapshot.child("ABOUT/phoneNumber").getValue(String.class),
                                        category);
                                taxiInfoHashMap.put(user, taxiInfo);
                            } else {
                                taxiInfo = taxiInfoHashMap.get(user);
                                taxiInfo.category = category;
                            }

                            addTaxiMarker(
                                    dataSnapshot.child("LOCATION/lat").getValue(Double.class),
                                    dataSnapshot.child("LOCATION/lon").getValue(Double.class),
                                    taxiInfo, user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void addTaxiMarker(Double lat, Double lon, TaxiInfo info, String user) {
        if (MapHandler.getMapHandler().itemizedLayer == null && timerForLocation != null)
            timerForLocation.cancel();

        if (MapHandlerAdmin.getMapHandler().itemizedLayer == null && timerForLocation != null) {
            timerForLocation.cancel();
            return;
        }

        MapHandlerAdmin.getMapHandler().itemizedLayer
                .addItem(MapHandlerAdmin.getMapHandler()
                        .createMarkerItem(activity, new GeoPoint(lat, lon), R.drawable.ic_my_location_dark_24dp,
                                0.5f, 1.0f, true, info, user));
    }

    protected void initViews() {
        this.showPositionBtn = activity.findViewById(R.id.map_show_my_position_fab);
        this.naviCenterBtn = activity.findViewById(R.id.map_southbar_navicenter_fab);

        // view groups managed by separate layout xml file : //map_sidebar_layout/map_sidebar_menu_layout
        this.sideBarVP = activity.findViewById(R.id.map_sidebar_layout);
        this.sideBarMenuVP = activity.findViewById(R.id.map_sidebar_menu_layout);

        //form location and to location textView
        this.menuVisible = false;

        Navigator.getNavigator().addListener(this);
    }

    /**
     * add end point marker to map
     *
     * @param endPoint
     */
    private void addToMarker(GeoPoint endPoint, boolean recalculate) {
        MapHandlerTaxi.getMapHandler().setStartEndPoint(activity, endPoint, false, recalculate);
    }

    /**
     * add start point marker to map
     *
     * @param startPoint
     */
    private void addFromMarker(GeoPoint startPoint, boolean recalculate) {
        MapHandlerTaxi.getMapHandler().setStartEndPoint(activity, startPoint, true, recalculate);
    }

    private void doSelectCurrentPos(GeoPoint newPos, String text, boolean isStartP) {
        if (isStartP) {
            Destination.getDestination().setStartPoint(newPos, text);
            addFromMarker(Destination.getDestination().getStartPoint(), true);
        } else {
            Destination.getDestination().setEndPoint(newPos, text);
            addToMarker(Destination.getDestination().getEndPoint(), true);
        }
        setQuickButtonsClearVisible(isStartP, true);
        MapHandlerTaxi.getMapHandler().centerPointOnMap(newPos, 0, 0, 0);
    }

    void setQuickButtonsClearVisible(boolean isStartP, boolean vis) {
        int curVis = View.VISIBLE;
        if (isStartP) {
            if (!vis) {
                curVis = View.INVISIBLE;
            }
            activity.findViewById(R.id.nav_settings_from_del_btn).setVisibility(curVis);
            if (vis) {
                curVis = View.INVISIBLE;
            } else {
                curVis = View.VISIBLE;
            }
            activity.findViewById(R.id.nav_settings_from_search_btn).setVisibility(curVis);
            activity.findViewById(R.id.nav_settings_from_fav_btn).setVisibility(curVis);
            activity.findViewById(R.id.nav_settings_from_cur_btn).setVisibility(curVis);
        } else {
            if (!vis) {
                curVis = View.INVISIBLE;
            }
            activity.findViewById(R.id.nav_settings_to_del_btn).setVisibility(curVis);
            if (vis) {
                curVis = View.INVISIBLE;
            } else {
                curVis = View.VISIBLE;
            }
            activity.findViewById(R.id.nav_settings_to_search_btn).setVisibility(curVis);
            activity.findViewById(R.id.nav_settings_to_fav_btn).setVisibility(curVis);
            activity.findViewById(R.id.nav_settings_to_sel_btn).setVisibility(curVis);
        }
    }

    /**
     * when use press on the screen to get a location form map
     *
     * @param latLong
     */
    @Override
    public void onPressLocation(GeoPoint latLong) {
        onPressLocationHelper(latLong);
        String text = "" + latLong.getLatitude() + ", " + latLong.getLongitude();
        doSelectCurrentPos(latLong, text, tabAction == TabAction.StartPoint);
        tabAction = TabAction.None;
    }

    protected void onPressLocationHelper(GeoPoint latLong) {
        if (tabAction == TabAction.None) {
            return;
        }
        if (tabAction == TabAction.AddFavourit) {
            sideBarVP.setVisibility(View.VISIBLE);
            tabAction = TabAction.None;
            GeoPoint[] points = new GeoPoint[3];
            points[2] = latLong;
            String[] names = new String[3];
            names[2] = "Seçilmiş məkan";
            startGeocodeActivity(points, names, false, true);
        }
    }

    /**
     * @return side bar menu visibility status
     */
    public boolean isMenuVisible() {
        return menuVisible;
    }

    /**
     * side bar menu visibility
     *
     * @param menuVisible
     */
    public void setMenuVisible(boolean menuVisible) {
        this.menuVisible = menuVisible;
    }

    /**
     * the change on navigator: navigation is used or not
     *
     * @param on
     */
    @Override
    public void onStatusChanged(boolean on) {
    }

    public OnClickListener createNaviCenterListener() {
        OnClickListener l = view -> {
            NaviEngine.getNaviEngine().setMapUpdatesAllowed(activity.getApplicationContext(), true);
            naviCenterBtn.setVisibility(View.INVISIBLE);
        };
        return l;
    }

    public Map.UpdateListener createUpdateListener() {
        Map.UpdateListener d = (e, mapPosition) -> {
            if (e == Map.MOVE_EVENT && NaviEngine.getNaviEngine().isNavigating()) {
                NaviEngine.getNaviEngine().setMapUpdatesAllowed(activity.getApplicationContext(), false);
            }
        };
        return d;
    }
}
