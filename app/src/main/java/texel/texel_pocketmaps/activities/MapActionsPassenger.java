package texel.texel_pocketmaps.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

import java.util.Date;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Order;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;
import texel.texel_pocketmaps.MyDialogs.DialogAskSomething;
import texel.texel_pocketmaps.Services.PassengerForegroundService;
import texel.texel_pocketmaps.activities.HelperActivities.PassengerTaxiChatActivity;
import texel.texel_pocketmaps.activities.SignActivities.SignIn;
import texel.texel_pocketmaps.fragments.AppSettings;
import texel.texel_pocketmaps.fragments.InstructionAdapter;
import texel.texel_pocketmaps.map.Destination;
import texel.texel_pocketmaps.map.MapHandlerPassenger;
import texel.texel_pocketmaps.map.MapHandlerTaxi;
import texel.texel_pocketmaps.map.Navigator;
import texel.texel_pocketmaps.model.listeners.MapHandlerListener;
import texel.texel_pocketmaps.model.listeners.NavigatorListener;
import texel.texel_pocketmaps.model.listeners.OnClickAddressListener;
import texel.texel_pocketmaps.navigator.NaviEngine;
import texel.texel_pocketmaps.util.Variable;


/**
 * This file is part of PocketMaps
 * <p>
 * menu controller, controls menus for map activity
 * <p>
 * Created by GuoJunjun <junjunguo.com> on June 24, 2015.
 */
public class MapActionsPassenger extends MapActions implements NavigatorListener, MapHandlerListener {
    public final static String EMPTY_LOC_STR = "..........";
    private final AppSettings appSettings;
    private final ViewGroup sideBarVP;
    private final ViewGroup sideBarMenuVP;
    private final ViewGroup southBarFavourVP;
    private final ViewGroup navSettingsVP;
    private final ViewGroup navSettingsFromVP;
    private final ViewGroup navSettingsToVP;
    private final ViewGroup navInstructionListVP;
    private final TextView fromLocalET;
    private final TextView toLocalET;
    private final Button nav_settings_order;
    private final CustomProgressDialog progressDialog;
    protected FloatingActionButton navigationBtn, logOutBtn, controlBtn, favourBtn, naviCenterBtn, chatBtn;
    private boolean menuVisible;
    private String order_name;

    public MapActionsPassenger(Activity activity, MapView mapView) {
        this.activity = activity;

        progressDialog = new CustomProgressDialog(activity, activity.getString(R.string.data_loading));
        progressDialog.setCancelable(false);

        this.showPositionBtn = activity.findViewById(R.id.map_show_my_position_fab);
        this.navigationBtn = activity.findViewById(R.id.map_nav_fab);
        this.favourBtn = activity.findViewById(R.id.map_southbar_favour_fab);
        this.controlBtn = activity.findViewById(R.id.map_sidebar_control_fab);
        this.naviCenterBtn = activity.findViewById(R.id.map_southbar_navicenter_fab);
        this.chatBtn = activity.findViewById(R.id.map_nav_chat);

        // view groups managed by separate layout xml file : //map_sidebar_layout/map_sidebar_menu_layout
        this.sideBarVP = activity.findViewById(R.id.map_sidebar_layout);
        this.sideBarMenuVP = activity.findViewById(R.id.map_sidebar_menu_layout);
        this.southBarFavourVP = activity.findViewById(R.id.map_southbar_favour_layout);
        this.navSettingsVP = activity.findViewById(R.id.nav_settings_layout);
        this.navSettingsFromVP = activity.findViewById(R.id.nav_settings_from_layout);
        this.navSettingsToVP = activity.findViewById(R.id.nav_settings_to_layout);
        this.navInstructionListVP = activity.findViewById(R.id.nav_instruction_list_layout);

        //form location and to location textView
        this.fromLocalET = activity.findViewById(R.id.nav_settings_from_local_et);
        this.toLocalET = activity.findViewById(R.id.nav_settings_to_local_et);
        this.menuVisible = false;

        this.logOutBtn = activity.findViewById(R.id.map_log_out);
        this.nav_settings_order = activity.findViewById(R.id.nav_settings_order);
        this.favourBtn = activity.findViewById(R.id.map_southbar_favour_fab);

        MapHandlerPassenger.getMapHandler().setMapHandlerListener(this);
        MapHandlerPassenger.getMapHandler().setNaviCenterBtn(naviCenterBtn);
        Navigator.getNavigator().addListener(this);

        appSettings = new AppSettings(activity);
        naviCenterBtn.setOnClickListener(createNaviCenterListener());
        initControlBtnHandler();
        initShowMyLocation();
        mapView.map().events.bind(createUpdateListener());
        initNavBtnHandler();
        initNavSettingsHandler();
        initFavourBtnHandler();
        initOrderButtonHandler();
        initChatBtnHandler();
        initLogOutButtonHandler();

        mapView.map().getEventLayer().enableRotation(true);
        mapView.map().getEventLayer().enableTilt(true);
    }

    private Map.UpdateListener createUpdateListener() {
        Map.UpdateListener d = (e, mapPosition) -> {
            if (e == Map.MOVE_EVENT && NaviEngine.getNaviEngine().isNavigating()) {
                NaviEngine.getNaviEngine().setMapUpdatesAllowed(activity.getApplicationContext(), false);
            }
        };
        return d;
    }

    public OnClickListener createNaviCenterListener() {
        OnClickListener l = view -> {
            NaviEngine.getNaviEngine().setMapUpdatesAllowed(activity.getApplicationContext(), true);
            naviCenterBtn.setVisibility(View.INVISIBLE);
        };
        return l;
    }

    /**
     * init and implement performance for favourites
     */
    @SuppressLint("ResourceAsColor")
    private void initFavourBtnHandler() {
        initPointOnMapHandler(TabAction.AddFavourit, R.id.map_southbar_favour_select_fab, true);

        favourBtn.setOnClickListener(new View.OnClickListener() {
            ColorStateList oriColor;

            @Override
            public void onClick(View v) {
                if (southBarFavourVP.getVisibility() == View.VISIBLE) {
                    favourBtn.setBackgroundTintList(oriColor);
                    southBarFavourVP.setVisibility(View.INVISIBLE);
                    sideBarMenuVP.setVisibility(View.VISIBLE);
                    controlBtn.setVisibility(View.VISIBLE);
                } else {
                    oriColor = favourBtn.getBackgroundTintList();
                    favourBtn.setBackgroundTintList(ColorStateList.valueOf(R.color.abc_color_highlight_material));
                    southBarFavourVP.setVisibility(View.VISIBLE);
                    sideBarMenuVP.setVisibility(View.INVISIBLE);
                    controlBtn.clearAnimation();
                    controlBtn.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    /**
     * navigation settings implementation
     * <p>
     * settings clear button
     * <p>
     * settings search button
     */
    private void initNavSettingsHandler() {
        final ImageButton navSettingsClearBtn = activity.findViewById(R.id.nav_settings_clear_btn);
        navSettingsClearBtn.setOnClickListener(v -> {
            navSettingsVP.setVisibility(View.INVISIBLE);
            sideBarVP.setVisibility(View.VISIBLE);
        });
        initSettingsFromItemHandler();
        initSettingsToItemHandler();
    }

    @SuppressWarnings("deprecation")
    private void setBgColor(View v, int color) {
        v.setBackgroundColor(activity.getResources().getColor(color));
    }

    /**
     * settings layout:
     * <p>
     * to item handler: when to item is clicked
     */
    private void initSettingsToItemHandler() {
        final ViewGroup toItemVG = activity.findViewById(R.id.map_nav_settings_to_item);
        toItemVG.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setBgColor(toItemVG, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    setBgColor(toItemVG, R.color.my_primary);
                    navSettingsVP.setVisibility(View.INVISIBLE);
                    navSettingsToVP.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });
        //        to layout
        //clear button
        ImageButton toLayoutClearBtn = activity.findViewById(R.id.nav_settings_to_clear_btn);
        toLayoutClearBtn.setOnClickListener(v -> {
            navSettingsVP.setVisibility(View.VISIBLE);
            navSettingsToVP.setVisibility(View.INVISIBLE);
        });
        //  to layout: items
        initUseCurrentLocationHandler(false, R.id.map_nav_settings_to_current, true);
        initPointOnMapHandler(TabAction.EndPoint, R.id.map_nav_settings_to_point, true);
        initPointOnMapHandler(TabAction.EndPoint, R.id.nav_settings_to_sel_btn, false);
        initEnterLatLonHandler(false, R.id.map_nav_settings_to_latlon);
        initClearCurrentLocationHandler(false, R.id.nav_settings_to_del_btn);
        initSearchLocationHandler(false, true, R.id.map_nav_settings_to_favorite, true);
        initSearchLocationHandler(false, false, R.id.map_nav_settings_to_search, true);
        initSearchLocationHandler(false, true, R.id.nav_settings_to_fav_btn, false);
        initSearchLocationHandler(false, false, R.id.nav_settings_to_search_btn, false);
    }

    /**
     * add end point marker to map
     *
     * @param endPoint
     */
    private void addToMarker(GeoPoint endPoint, boolean recalculate) {
        MapHandlerPassenger.getMapHandler().setStartEndPoint(activity, endPoint, false, recalculate);
    }

    /**
     * add start point marker to map
     *
     * @param startPoint
     */
    private void addFromMarker(GeoPoint startPoint, boolean recalculate) {
        MapHandlerPassenger.getMapHandler().setStartEndPoint(activity, startPoint, true, recalculate);
    }

    /**
     * settings layout:
     * <p>
     * from item handler: when from item is clicked
     */
    private void initSettingsFromItemHandler() {
        final ViewGroup fromFieldVG = activity.findViewById(R.id.map_nav_settings_from_item);
        fromFieldVG.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setBgColor(fromFieldVG, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    setBgColor(fromFieldVG, R.color.my_primary);
                    navSettingsVP.setVisibility(View.INVISIBLE);
                    navSettingsFromVP.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        });
        ImageButton fromLayoutClearBtn = activity.findViewById(R.id.nav_settings_from_clear_btn);
        fromLayoutClearBtn.setOnClickListener(v -> {
            navSettingsVP.setVisibility(View.VISIBLE);
            navSettingsFromVP.setVisibility(View.INVISIBLE);
        });
        initUseCurrentLocationHandler(true, R.id.map_nav_settings_from_current, true);
        initUseCurrentLocationHandler(true, R.id.nav_settings_from_cur_btn, false);
        initEnterLatLonHandler(true, R.id.map_nav_settings_from_latlon);
        initClearCurrentLocationHandler(true, R.id.nav_settings_from_del_btn);
        initPointOnMapHandler(TabAction.StartPoint, R.id.map_nav_settings_from_point, true);
        initSearchLocationHandler(true, true, R.id.map_nav_settings_from_favorite, true);
        initSearchLocationHandler(true, false, R.id.map_nav_settings_from_search, true);
        initSearchLocationHandler(true, true, R.id.nav_settings_from_fav_btn, false);
        initSearchLocationHandler(true, false, R.id.nav_settings_from_search_btn, false);
    }

    /**
     * Point item view group
     * <p>
     * preform actions when point on map item is clicked
     */
    private void initPointOnMapHandler(final TabAction tabType, int viewID, final boolean setBg) {
        final View pointItem = activity.findViewById(viewID);
        pointItem.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (setBg) setBgColor(pointItem, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (setBg) setBgColor(pointItem, R.color.my_primary);
                    if (tabType == TabAction.StartPoint) { //touch on map
                        tabAction = TabAction.StartPoint;
                        navSettingsFromVP.setVisibility(View.INVISIBLE);
                        Toast.makeText(activity, "Başlanğıc ÜNVANı seçmək üçün xəritəyə toxunun",
                                Toast.LENGTH_SHORT).show();
                    } else if (tabType == TabAction.EndPoint) {
                        tabAction = TabAction.EndPoint;
                        navSettingsToVP.setVisibility(View.INVISIBLE);
                        Toast.makeText(activity, "Gedəcəyiniz ÜNVANı seçmək üçün xəritəyə toxunun",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        tabAction = TabAction.AddFavourit;
                        sideBarVP.setVisibility(View.INVISIBLE);
                        Toast.makeText(activity, "ÜNVANı seçmək üçün xəritəyə toxunun",
                                Toast.LENGTH_SHORT).show();
                    }
                    navSettingsVP.setVisibility(View.INVISIBLE);
                    MapHandlerPassenger.getMapHandler().setNeedLocation(true);
                    return true;
            }
            return false;
        });
    }

    private void initEnterLatLonHandler(final boolean isStartP, int viewID) {
        final View pointItem = activity.findViewById(viewID);
        pointItem.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setBgColor(pointItem, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    setBgColor(pointItem, R.color.my_primary);
                    Intent intent = new Intent(activity, LatLonActivity.class);
                    OnClickAddressListener callbackListener = createPosSelectedListener(isStartP);
                    LatLonActivity.setPre(callbackListener);
                    activity.startActivity(intent);
                    return true;
            }
            return false;
        });
    }

    private void initSearchLocationHandler(final boolean isStartP, final boolean fromFavourite, int viewID, final boolean setBg) {
        final View pointItem = activity.findViewById(viewID);
        pointItem.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (setBg) setBgColor(pointItem, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (setBg) setBgColor(pointItem, R.color.my_primary);
                    GeoPoint[] points = null;
                    String[] names = null;
                    if (fromFavourite) {
                        points = new GeoPoint[3];
                        points[0] = Destination.getDestination().getStartPoint();
                        points[1] = Destination.getDestination().getEndPoint();
                        names = new String[2];
                        names[0] = Destination.getDestination().getStartPointName();
                        names[1] = Destination.getDestination().getEndPointName();
                        Location curLoc = MapActivity.getmCurrentLocation();
                        if (curLoc != null) {
                            points[2] = new GeoPoint(curLoc.getLatitude(), curLoc.getLongitude());
                        }
                    }
                    startGeocodeActivity(points, names, isStartP, false);
                    return true;
            }
            return false;
        });
    }

    /**
     * Shows the GeocodeActivity, or Favourites, if points are not null.
     *
     * @param points The points to add as favourites, [0]=start [1]=end [2]=cur.
     **/
    public void startGeocodeActivity(GeoPoint[] points, String[] names, boolean isStartP, boolean autoEdit) {
        Intent intent = new Intent(activity, GeocodeActivity.class);
        OnClickAddressListener callbackListener = createPosSelectedListener(isStartP);
        GeocodeActivity.setPre(callbackListener, points, names, autoEdit);
        activity.startActivity(intent);
    }

    private OnClickAddressListener createPosSelectedListener(final boolean isStartP) {
        return addr -> {
            GeoPoint newPos = new GeoPoint(addr.getLatitude(), addr.getLongitude());
            String fullAddress = "";
            for (int i = 0; i < 5; i++) {
                String curAddr = addr.getAddressLine(i);
                if (curAddr == null || curAddr.isEmpty()) {
                    continue;
                }
                if (!fullAddress.isEmpty()) {
                    fullAddress = fullAddress + ", ";
                }
                fullAddress = fullAddress + curAddr;
            }
            doSelectCurrentPos(newPos, fullAddress, isStartP);
        };
    }

    private void doSelectCurrentPos(GeoPoint newPos, String text, boolean isStartP) {
        if (isStartP) {
            Destination.getDestination().setStartPoint(newPos, text);
            fromLocalET.setText(text);
            addFromMarker(Destination.getDestination().getStartPoint(), true);
            navSettingsFromVP.setVisibility(View.INVISIBLE);
        } else {
            Destination.getDestination().setEndPoint(newPos, text);
            toLocalET.setText(text);
            addToMarker(Destination.getDestination().getEndPoint(), true);
            navSettingsToVP.setVisibility(View.INVISIBLE);
        }
        setQuickButtonsClearVisible(isStartP, true);
        sideBarVP.setVisibility(View.INVISIBLE);
        if (!activateNavigator()) {
            navSettingsVP.setVisibility(View.VISIBLE);
        }
//        MapHandlerPassenger.getMapHandler().centerPointOnMap(newPos, 0, 0, 0);
    }

    /**
     * current location handler: preform actions when current location item is clicked
     */
    private void initUseCurrentLocationHandler(final boolean isStartP, int viewID, final boolean setBg) {
        final View useCurrentLocal = activity.findViewById(viewID);
        useCurrentLocal.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (setBg) setBgColor(useCurrentLocal, R.color.my_primary_light);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (setBg) setBgColor(useCurrentLocal, R.color.my_primary);
                    if (MapActivity.getmCurrentLocation() != null) {
                        GeoPoint newPos = new GeoPoint(MapActivity.getmCurrentLocation().getLatitude(),
                                MapActivity.getmCurrentLocation().getLongitude());
                        String text = newPos.getLatitude() + ", " + newPos.getLongitude();
                        doSelectCurrentPos(newPos, text, isStartP);
                    } else {
                        Toast.makeText(activity, "Məkan aktiv deyil, Məkanı(GPS) yoxla!",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
            return false;
        });
    }

    /**
     * current location handler: preform actions when clear location item is clicked
     */
    private void initClearCurrentLocationHandler(final boolean isStartP, int viewID) {
        final View useCurrentLocal = activity.findViewById(viewID);
        useCurrentLocal.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (isStartP) {
                        Destination.getDestination().setStartPoint(null, null);
                        addFromMarker(null, false);
                        fromLocalET.setText(EMPTY_LOC_STR);
                    } else {
                        Destination.getDestination().setEndPoint(null, null);
                        addToMarker(null, false);
                        toLocalET.setText(EMPTY_LOC_STR);
                    }
                    setQuickButtonsClearVisible(isStartP, false);
                    return true;
            }
            return false;
        });
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
        if (tabAction == TabAction.None) {
            return;
        }
        if (tabAction == TabAction.AddFavourit) {
            sideBarVP.setVisibility(View.VISIBLE);
            tabAction = TabAction.None;
            GeoPoint[] points = new GeoPoint[3];
            points[2] = latLong;
            String[] names = new String[3];
            names[2] = "Selected position";
            startGeocodeActivity(points, names, false, true);
            return;
        }
        String text = "" + latLong.getLatitude() + ", " + latLong.getLongitude();
        doSelectCurrentPos(latLong, text, tabAction == TabAction.StartPoint);
        tabAction = TabAction.None;
    }

    @Override
    public void pathCalculating(boolean calculatingPathActive) {
        if (!calculatingPathActive && Navigator.getNavigator().getGhResponse() != null) {
            if (!NaviEngine.getNaviEngine().isNavigating()) {
                activateDirections();
            }
        }
    }

    /**
     * drawer polyline on map , active navigator instructions(directions) if on
     *
     * @return True when pathfinder-routes will be shown.
     */
    private boolean activateNavigator() {
        GeoPoint startPoint = Destination.getDestination().getStartPoint();
        GeoPoint endPoint = Destination.getDestination().getEndPoint();
        if (startPoint != null && endPoint != null) {
            // show path finding process
            navSettingsVP.setVisibility(View.INVISIBLE);

            if (Variable.getVariable().isDirectionsON()) {
                MapHandlerPassenger.getMapHandler().setNeedPathCal(true);
                // Waiting for calculating
            }
            return true;
        }
        return false;
    }

    /**
     * active directions, and directions view
     */
    private void activateDirections() {
        RecyclerView instructionsRV;
        RecyclerView.Adapter<?> instructionsAdapter;
        RecyclerView.LayoutManager instructionsLayoutManager;

        instructionsRV = (RecyclerView) activity.findViewById(R.id.nav_instruction_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        instructionsRV.setHasFixedSize(true);

        // use a linear layout manager
        instructionsLayoutManager = new LinearLayoutManager(activity);
        instructionsRV.setLayoutManager(instructionsLayoutManager);

        // specify an adapter (see also next example)
        instructionsAdapter = new InstructionAdapter(Navigator.getNavigator().getGhResponse().getInstructions());
        instructionsRV.setAdapter(instructionsAdapter);
        initNavListView();
    }

    /**
     * navigation list view
     * <p>
     * make nav list view control button ready to use
     */
    private void initNavListView() {
        fillNavListSummaryValues();
        navSettingsVP.setVisibility(View.INVISIBLE);
        navInstructionListVP.setVisibility(View.VISIBLE);
        ImageButton clearBtn, stopBtn, stopNavBtn;
        stopBtn = activity.findViewById(R.id.nav_instruction_list_stop_btn);
        clearBtn = activity.findViewById(R.id.nav_instruction_list_clear_btn);
        stopNavBtn = activity.findViewById(R.id.navtop_stop);
        stopBtn.setOnClickListener(v -> {
            // 1. Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage(R.string.stop_navigation_msg).setTitle(R.string.stop_navigation)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        // stop!
                        Navigator.getNavigator().setOn(false);
                        navInstructionListVP.setVisibility(View.INVISIBLE);
                        navSettingsVP.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                    }).setNegativeButton(R.string.no, (dialog, id) -> {
                // User cancelled the dialog
                dialog.dismiss();
            });
            // Create the AlertDialog object and return it

            // 3. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        clearBtn.setOnClickListener(v -> {
            navInstructionListVP.setVisibility(View.INVISIBLE);
            navSettingsVP.setVisibility(View.INVISIBLE);
            sideBarVP.setVisibility(View.VISIBLE);
        });

        stopNavBtn.setOnClickListener(v -> Navigator.getNavigator().setNaviStart(activity, false));
    }

    /**
     * fill up values for nav list summary
     */
    private void fillNavListSummaryValues() {
        TextView from, to, distance, time, money;
        from = activity.findViewById(R.id.nav_instruction_list_summary_from_tv);
        to = activity.findViewById(R.id.nav_instruction_list_summary_to_tv);
        distance = activity.findViewById(R.id.nav_instruction_list_summary_distance_tv);
        money = activity.findViewById(R.id.nav_instruction_list_money);
        time = activity.findViewById(R.id.nav_instruction_list_summary_time_tv);

        from.setText(Destination.getDestination().getStartPointToString());
        to.setText(Destination.getDestination().getEndPointToString());
        distance.setText(Navigator.getNavigator().getDistanceByStringWithUnit());
        time.setText(Navigator.getNavigator().getTime());
        MapHandlerTaxi.getMapHandler().calcPath(
                Destination.getDestination().getStartPoint().getLatitude(),
                Destination.getDestination().getStartPoint().getLongitude(),
                Destination.getDestination().getEndPoint().getLatitude(),
                Destination.getDestination().getEndPoint().getLongitude(),
                activity,
                null,
                null,
                money);
    }

    /**
     * handler clicks on nav button
     */
    private void initNavBtnHandler() {
        navigationBtn.setOnClickListener(v -> {
            sideBarVP.setVisibility(View.INVISIBLE);
            if (Navigator.getNavigator().isOn()) {
                navInstructionListVP.setVisibility(View.VISIBLE);
            } else {
                navSettingsVP.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * start button: control button handler FAB
     */

    private void initControlBtnHandler() {
        final ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1);
        anim.setFillBefore(true);
        anim.setFillAfter(true);
        anim.setFillEnabled(true);
        anim.setDuration(300);
        anim.setInterpolator(new OvershootInterpolator());

        controlBtn.setOnClickListener(v -> {
            if (isMenuVisible()) {
                setMenuVisible(false);
                sideBarMenuVP.setVisibility(View.INVISIBLE);
                favourBtn.setVisibility(View.INVISIBLE);
                controlBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
            } else {
                setMenuVisible(true);
                sideBarMenuVP.setVisibility(View.VISIBLE);
                favourBtn.setVisibility(View.VISIBLE);
                controlBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
            }
            controlBtn.startAnimation(anim);
        });
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
        if (on) {
            navigationBtn.setImageResource(R.drawable.ic_directions_white_24dp);
        } else {
            navigationBtn.setImageResource(R.drawable.ic_navigation_white_24dp);
        }
    }

    @Override
    public void onNaviStart(boolean on) {
        navInstructionListVP.setVisibility(View.INVISIBLE);
        navSettingsVP.setVisibility(View.INVISIBLE);
        if (on) {
            sideBarVP.setVisibility(View.INVISIBLE);
        } else {
            sideBarVP.setVisibility(View.VISIBLE);
        }
    }

    /**
     * called from Map activity when onBackpressed
     *
     * @return false no actions will perform; return true MapActivity will be placed pages_background in the activity stack
     */
    public boolean homeBackKeyPressed() {
        if (navSettingsVP.getVisibility() == View.VISIBLE) {
            navSettingsVP.setVisibility(View.INVISIBLE);
            sideBarVP.setVisibility(View.VISIBLE);
            return false;
        } else if (navSettingsFromVP.getVisibility() == View.VISIBLE) {
            navSettingsFromVP.setVisibility(View.INVISIBLE);
            navSettingsVP.setVisibility(View.VISIBLE);
            return false;
        } else if (navSettingsToVP.getVisibility() == View.VISIBLE) {
            navSettingsToVP.setVisibility(View.INVISIBLE);
            navSettingsVP.setVisibility(View.VISIBLE);
            return false;
        } else if (navInstructionListVP.getVisibility() == View.VISIBLE) {
            navInstructionListVP.setVisibility(View.INVISIBLE);
            sideBarVP.setVisibility(View.VISIBLE);
            return false;
        } else if (appSettings.getAppSettingsVP() != null &&
                appSettings.getAppSettingsVP().getVisibility() == View.VISIBLE) {
            appSettings.getAppSettingsVP().setVisibility(View.INVISIBLE);
            sideBarVP.setVisibility(View.VISIBLE);
            return false;
        } else if (NaviEngine.getNaviEngine().isNavigating()) {
            Navigator.getNavigator().setNaviStart(activity, false);
            return false;
        } else if (southBarFavourVP.getVisibility() == View.VISIBLE) {
            favourBtn.callOnClick();
            return false;
        } else {
            return true;
        }
    }

    protected void initLogOutButtonHandler() {
        logOutBtn.setOnClickListener(v -> {
            View.OnClickListener positiveButtonListener = (view) -> {
                FirebaseAuth.getInstance().signOut();
                activity.finish();
                activity.startActivity(new Intent(activity, SignIn.class));
            };
            View.OnClickListener neutralButtonListener = (view) -> {
                DialogAskSomething.alertDialog.dismiss();
            };
            DialogAskSomething dialogAskSomething =
                    new DialogAskSomething(activity.getString(R.string.want_log_out),
                            activity.getString(R.string.yes), "", activity.getString(R.string.no),
                            positiveButtonListener, null, neutralButtonListener, false);
            dialogAskSomething.show(activity);
        });
    }

    public void initChatBtnHandler() {
        chatBtn.setOnClickListener(v -> {
            if (PassengerForegroundService.isActive) {
                Intent intent = new Intent(activity, PassengerTaxiChatActivity.class);
                intent.putExtra("order_name", order_name);
                activity.startActivity(intent);
            } else chatBtn.setVisibility(View.GONE);
        });
    }

    private void initOrderButtonHandler() {
        nav_settings_order.setOnClickListener(v -> {
            if (Destination.getDestination().getStartPoint() == null || Destination.getDestination().getEndPoint() == null)
                return;
            showPermissionToOrderDialog();
        });
    }

    private void showPermissionToOrderDialog() {
        View.OnClickListener positiveButtonListener = (view) -> {
            DialogAskSomething.alertDialog.dismiss();
            startOrder();
        };
        View.OnClickListener neutralButtonListener = (view) -> {
            DialogAskSomething.alertDialog.dismiss();
        };
        DialogAskSomething dialogAskSomething =
                new DialogAskSomething("Sifariş verilsinmi?",
                        activity.getString(R.string.yes), "", activity.getString(R.string.no),
                        positiveButtonListener, null, neutralButtonListener, true);
        dialogAskSomething.show(activity);
    }

    private void startOrder() {
        progressDialog.show();

        navSettingsVP.setVisibility(View.INVISIBLE);
        sideBarVP.setVisibility(View.VISIBLE);
        chatBtn.setVisibility(View.VISIBLE);
        navInstructionListVP.setVisibility(View.INVISIBLE);

        if (PassengerForegroundService.isActive) {
            Toast.makeText(activity, "Sizin aktiv sifarişiniz var!!!", Toast.LENGTH_LONG).show();
            return;
        }
        final String userName = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0];
        String currentDateTime = MyHelperFunctions.dateFormatter.format(new Date());
        order_name = currentDateTime + " " + userName;

        DatabaseFunctions.getDatabases(activity).get(0)
                .child("USERS/PASSENGER/" + userName + "/ABOUT/name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Order order = new Order(
                                1,
                                snapshot.getValue(String.class),
                                Destination.getDestination().getStartPoint().getLatitude(),
                                Destination.getDestination().getStartPoint().getLongitude(),
                                Destination.getDestination().getEndPoint().getLatitude(),
                                Destination.getDestination().getEndPoint().getLongitude());

                        DatabaseFunctions.getDatabases(activity).get(1).child("ORDERS/ACTIVE/" + order_name).setValue(order);
                        PassengerForegroundService.orderName = order_name;

                        progressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(activity, R.string.error_check_internet, Toast.LENGTH_LONG).show();
                        Log.d("AAAAAAAA", error.getMessage());
                    }
                });
        Toast.makeText(activity, "Sorğunuz tezliklə cavablanacaqdır. Zəhmət olmazsa, Gözləyin...", Toast.LENGTH_LONG).show();
        startPassengerService(order_name);
    }

    private void startPassengerService(String orderName) {
        PassengerForegroundService.orderNotificationStep = 1;
        Intent serviceIntent = new Intent(activity, PassengerForegroundService.class);
        serviceIntent.putExtra("order_name", orderName);
        ContextCompat.startForegroundService(activity, serviceIntent);
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
}