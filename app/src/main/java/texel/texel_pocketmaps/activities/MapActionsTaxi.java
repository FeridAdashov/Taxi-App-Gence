package texel.texel_pocketmaps.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.DialogAskSomething;
import texel.texel_pocketmaps.Services.TaxiForegroundService;
import texel.texel_pocketmaps.activities.HelperActivities.PassengerTaxiChatActivity;
import texel.texel_pocketmaps.activities.SignActivities.SignIn;
import texel.texel_pocketmaps.fragments.AppSettings;
import texel.texel_pocketmaps.fragments.InstructionAdapter;
import texel.texel_pocketmaps.map.Destination;
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
public class MapActionsTaxi extends MapActions implements NavigatorListener, MapHandlerListener {

    private final DatabaseReference databaseReferenceOrders;
    private final DatabaseReference databaseReferenceTaxi;
    private final String adminUsername;
    private final String adminPassword;
    protected FloatingActionButton navigationBtn, cancelOrderBtn, controlBtn,
            naviCenterBtn, logOutBtn, callAdmin,
            contactWithPassengerBtn, chatWithPassengerBtn, callPassengerBtn, statusBtn;
    protected ImageButton activateAccount;
    protected ViewGroup sideBarVP, sideBarMenuVP, navTopVP, navNewOrder, navContactWithPassenger, navTaxiInfo;
    protected boolean menuVisible;
    protected TextView fromLocalET, toLocalET, textViewTaxiInfoNavActiveness, textViewTaxiInfoNavQN, textViewTaxiInfoNavBalance;
    boolean b = false;
    private Button buttonNewOrderSubmit;
    private Timer timerForNewOrder;
    private Boolean active;
    private String dialogMessage;
    private Double balance;

    public MapActionsTaxi(Activity activity, MapView mapView) {
        this.activity = activity;
        initViews();

        adminUsername = activity.getIntent().getStringExtra("adminUsername");
        adminPassword = activity.getIntent().getStringExtra("adminPassword");

        databaseReferenceOrders = DatabaseFunctions.getDatabases(activity).get(1);
        databaseReferenceTaxi = DatabaseFunctions.getDatabases(activity).get(0)
                .child("USERS/TAXI/" + FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0]);

        MapHandlerTaxi.getMapHandler().setMapHandlerListener(this);
        MapHandlerTaxi.getMapHandler().setNaviCenterBtn(naviCenterBtn);

        appSettings = new AppSettings(activity);
        naviCenterBtn.setOnClickListener(createNaviCenterListener());
        initControlBtnHandler();
        initShowMyLocation();
        initLogOutButtonHandler();
        initActivateAccountHandler();
        initCallAdminHandler();
        initNavBtnHandler();
        initStatusBtnHandler();
        initCancelBtnHandler();
        initShowMyLocation();
        initNewOrderLayout();
        initContactWithPassengerButtons();
        initNavListView();

        mapView.map().events.bind(createUpdateListener());
        mapView.map().getEventLayer().enableRotation(true);
        mapView.map().getEventLayer().enableTilt(true);
    }

    protected void initViews() {
        this.showPositionBtn = activity.findViewById(R.id.map_show_my_position_fab);
        this.navigationBtn = activity.findViewById(R.id.map_nav_fab);
        this.cancelOrderBtn = activity.findViewById(R.id.map_nav_cancel_order);
        this.controlBtn = activity.findViewById(R.id.map_sidebar_control_fab);
        this.naviCenterBtn = activity.findViewById(R.id.map_southbar_navicenter_fab);

        this.contactWithPassengerBtn = activity.findViewById(R.id.map_nav_contact_with_passenger_button);
        this.chatWithPassengerBtn = activity.findViewById(R.id.map_nav_chat);
        this.callPassengerBtn = activity.findViewById(R.id.map_call_passenger);

        this.statusBtn = activity.findViewById(R.id.map_nav_status);

        // view groups managed by separate layout xml file : //map_sidebar_layout/map_sidebar_menu_layout
        this.sideBarVP = activity.findViewById(R.id.map_sidebar_layout);
        this.sideBarMenuVP = activity.findViewById(R.id.map_sidebar_menu_layout);
        this.navTopVP = activity.findViewById(R.id.navtop_layout);
        this.navNewOrder = activity.findViewById(R.id.nav_new_order_layout);
        this.navContactWithPassenger = activity.findViewById(R.id.map_nav_contact_with_passenger_layout);
        this.navTaxiInfo = activity.findViewById(R.id.navTaxiInfo);

        //form location and to location textView
        this.fromLocalET = activity.findViewById(R.id.nav_settings_from_local_et);
        this.toLocalET = activity.findViewById(R.id.nav_settings_to_local_et);
        this.textViewTaxiInfoNavActiveness = activity.findViewById(R.id.textViewTaxiInfoNavActiveness);
        this.textViewTaxiInfoNavQN = activity.findViewById(R.id.textViewTaxiInfoNavQN);
        this.textViewTaxiInfoNavBalance = activity.findViewById(R.id.textViewTaxiInfoNavBalance);
        this.menuVisible = false;

        Navigator.getNavigator().addListener(this);
    }

    private void initStatusBtnHandler() {
        statusBtn.setOnClickListener(v -> {
            if (TaxiForegroundService.orderStatus == 3)
                dialogMessage = "1. Müştəriyə çatmağınızla bağlı xəbərdarlıq göndəriləcək\n" +
                        "2. Son təyinat nöqtəsinə naviqasiya təyin ediləcək.";
            else dialogMessage = "Sifarişi tamamla!!!";

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(dialogMessage)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (TaxiForegroundService.orderStatus == 3) {
                            databaseReferenceOrders
                                    .child("ORDERS/ACTIVE/" + TaxiForegroundService.orderName + "/type")
                                    .setValue(++TaxiForegroundService.orderStatus);
                            createOrderPath(true, false, true);
                        } else {
                            databaseReferenceOrders
                                    .child("ORDERS/ACTIVE/" + TaxiForegroundService.orderName)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            databaseReferenceOrders
                                                    .child("ORDERS/COMPLETED/" + new SimpleDateFormat("dd_MM_yyyy").format(new Date())
                                                            + "/" + TaxiForegroundService.orderName)
                                                    .setValue(snapshot.getValue());

                                            databaseReferenceTaxi.child("orderName").setValue("");

                                            snapshot.getRef().removeValue();

                                            TaxiForegroundService.reset();
                                            resetMarkers();
                                            stopTaxiService();
                                            startTaxiService();
                                            startTimerForNewOrder();

                                            Toast.makeText(activity, "Sifariş tamamlandı.", Toast.LENGTH_LONG).show();
                                            unVisibleButtons();

                                            catchInterest();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                        dialog.dismiss();
                    }).setNegativeButton(R.string.no, (dialog, id) -> dialog.dismiss());
            builder.create().show();
        });
    }

    private void catchInterest() {
        DatabaseFunctions.getDatabases(activity).get(0).child("SETTING/taxiPercent").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double percent = snapshot.getValue(Double.class);

                TextView textViewMoney = activity.findViewById(R.id.money_for_taxi);
                String s_money = textViewMoney.getText().toString();
                s_money = s_money.substring(0, s_money.indexOf(" "));
                Double d_money = Double.parseDouble(s_money);
                
                if (percent != null && percent > 0. && d_money != null) {
                    balance -= (d_money * percent) / 100;
                    databaseReferenceTaxi.child("ABOUT/balance").setValue(balance);
                    textViewTaxiInfoNavBalance.setText(String.format("%.2f", balance) + " " + activity.getString(R.string.azn));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initContactWithPassengerButtons() {
        contactWithPassengerBtn.setOnClickListener(v -> {
            if (navContactWithPassenger.getVisibility() == View.VISIBLE)
                navContactWithPassenger.setVisibility(View.INVISIBLE);
            else navContactWithPassenger.setVisibility(View.VISIBLE);

            if (isMenuVisible()) controlBtn.callOnClick();
        });

        chatWithPassengerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(activity, PassengerTaxiChatActivity.class);
            intent.putExtra("order_name", TaxiForegroundService.orderName);
            activity.startActivity(intent);
            navContactWithPassenger.setVisibility(View.INVISIBLE);
        });

        callPassengerBtn.setOnClickListener(v -> DatabaseFunctions.getDatabases(activity).get(0)
                .child("USERS/PASSENGER/" + TaxiForegroundService.orderName.split(" ")[1] + "/ABOUT/phoneNumber")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phoneNumber = snapshot.getValue(String.class);
                        if (phoneNumber == null) return;

                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phoneNumber));
                        activity.startActivity(intent);
                        navContactWithPassenger.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }));
    }

    private void initNewOrderLayout() {
        buttonNewOrderSubmit = navNewOrder.findViewById(R.id.nav_new_order_submit_button);
        buttonNewOrderSubmit.setOnClickListener(v -> {
            TaxiForegroundService.timerForOrderSeconds.cancel();
            navNewOrder.setVisibility(View.INVISIBLE);
            cancelOrderBtn.setVisibility(View.VISIBLE);
            contactWithPassengerBtn.setVisibility(View.VISIBLE);
            statusBtn.setVisibility(View.VISIBLE);
            navTaxiInfo.setVisibility(View.INVISIBLE);
            timerForNewOrder.cancel();

            DatabaseFunctions.getDatabases(activity).get(1)
                    .child("ORDERS/ACTIVE/" + TaxiForegroundService.orderName + "/type").setValue(3);
            TaxiForegroundService.orderStatus = 3;
            createOrderPath(true, true, false);
        });

        databaseReferenceTaxi.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean b = snapshot.child("active").getValue(Boolean.class);
                String orderName = snapshot.child("orderName").getValue(String.class);
                String qN = snapshot.child("ABOUT/registrationNumber").getValue(String.class);
                balance = snapshot.child("ABOUT/balance").getValue(Double.class);

                if (balance == null) balance = 0.;

                textViewTaxiInfoNavQN.setText(qN);
                textViewTaxiInfoNavBalance.setText(String.format("%.2f", balance) + " " + activity.getString(R.string.azn));

                if (b != null && b) {
                    navTaxiInfo.setBackgroundColor(ContextCompat.getColor(activity, R.color.nav_taxi_info_color_active));
                    textViewTaxiInfoNavActiveness.setText(R.string.deactivate);
                    if (!TaxiForegroundService.isServiceActive) startTaxiService();
                }

                if (!TextUtils.isEmpty(orderName)) {
                    TaxiForegroundService.haveOrder = true;
                    TaxiForegroundService.orderName = orderName;

                    databaseReferenceOrders.child("ORDERS/ACTIVE/" + orderName)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Integer type = snapshot.child("type").getValue(Integer.class);
                                        TaxiForegroundService.orderStartLat = snapshot.child("p1Lat").getValue(Double.class);
                                        TaxiForegroundService.orderStartLon = snapshot.child("p1Lon").getValue(Double.class);
                                        TaxiForegroundService.orderEndLat = snapshot.child("p2Lat").getValue(Double.class);
                                        TaxiForegroundService.orderEndLon = snapshot.child("p2Lon").getValue(Double.class);

                                        if (type != null) {
                                            TaxiForegroundService.orderStatus = type;
                                            if (type == 2) startTimerForNewOrder();
                                            else {
                                                statusBtn.setVisibility(View.VISIBLE);
                                                navNewOrder.setVisibility(View.INVISIBLE);
                                                cancelOrderBtn.setVisibility(View.VISIBLE);
                                                contactWithPassengerBtn.setVisibility(View.VISIBLE);

                                                if (type == 3)
                                                    createOrderPath(true, true, false);
                                                else if (type == 4)
                                                    createOrderPath(true, false, true);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                } else startTimerForNewOrder();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createOrderPath(boolean createNavigation, boolean setFirstPath, boolean setSecondPath) {
        if (TaxiForegroundService.orderStartLat == 0. || TaxiForegroundService.orderStartLon == 0.
                || TaxiForegroundService.orderEndLat == 0. || TaxiForegroundService.orderEndLon == 0.)
            return;

        Variable.getVariable().setFirstPath = setFirstPath;
        Variable.getVariable().setSecondPath = setSecondPath;
        Variable.getVariable().createNavigation = createNavigation;

        resetMarkers();

        if (setFirstPath) {
            doSelectCurrentPos(
                    new GeoPoint(MapActivityTaxi.getmCurrentLocation().getLatitude(), MapActivityTaxi.getmCurrentLocation().getLongitude()), "MyLocation", true);

            doSelectCurrentPos(
                    new GeoPoint(TaxiForegroundService.orderStartLat, TaxiForegroundService.orderStartLon), "PassengerLocation", false);
        } else if (setSecondPath) {
            doSelectCurrentPos(
                    new GeoPoint(MapActivityTaxi.getmCurrentLocation().getLatitude(), MapActivityTaxi.getmCurrentLocation().getLongitude()), "MyLocation", true);

            doSelectCurrentPos(
                    new GeoPoint(TaxiForegroundService.orderEndLat, TaxiForegroundService.orderEndLon), "Son", false);
        }
    }

    private void resetMarkers() {
        Navigator.getNavigator().setNaviStart(activity, false);
        addToMarker(null, false);
        addFromMarker(null, false);
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
        activateNavigator();
        MapHandlerTaxi.getMapHandler().centerPointOnMap(newPos, 0, 0, 0);
    }

    private void startTimerForNewOrder() {
        if (timerForNewOrder != null) timerForNewOrder.cancel();

        timerForNewOrder = new Timer();
        timerForNewOrder.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TaxiForegroundService.haveOrder) {
                    b = true;
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            if (navNewOrder.getVisibility() != View.VISIBLE) {
                                cancelOrderBtn.setVisibility(View.GONE);
                                contactWithPassengerBtn.setVisibility(View.GONE);
                                statusBtn.setVisibility(View.GONE);
                                navNewOrder.setVisibility(View.VISIBLE);
                                navTaxiInfo.setVisibility(View.INVISIBLE);

                                MapHandlerTaxi.getMapHandler().calcPath(
                                        MapActivityTaxi.getmCurrentLocation().getLatitude(),
                                        MapActivityTaxi.getmCurrentLocation().getLongitude(),
                                        TaxiForegroundService.orderStartLat,
                                        TaxiForegroundService.orderStartLon,
                                        activity,
                                        activity.findViewById(R.id.distance_to_passenger),
                                        activity.findViewById(R.id.time_to_passenger),
                                        null);

                                MapHandlerTaxi.getMapHandler().calcPath(
                                        TaxiForegroundService.orderStartLat,
                                        TaxiForegroundService.orderStartLon,
                                        TaxiForegroundService.orderEndLat,
                                        TaxiForegroundService.orderEndLon,
                                        activity,
                                        activity.findViewById(R.id.distance_from_passenger_to_destination),
                                        activity.findViewById(R.id.time_from_passenger_to_destination),
                                        activity.findViewById(R.id.money_for_taxi));
                            } else
                                buttonNewOrderSubmit.setText("Qəbul Et (" + TaxiForegroundService.activeSeconds + " san.)");
                        }
                    });
                } else if (b) {
                    b = false;
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            navNewOrder.setVisibility(View.INVISIBLE);
                            navTaxiInfo.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }, 0, 1000);
    }

    private void unVisibleButtons() {
        navTopVP.setVisibility(View.INVISIBLE);
        navNewOrder.setVisibility(View.INVISIBLE);
        navigationBtn.setVisibility(View.GONE);
        cancelOrderBtn.setVisibility(View.GONE);
        contactWithPassengerBtn.setVisibility(View.GONE);
        statusBtn.setVisibility(View.GONE);
        navTaxiInfo.setVisibility(View.VISIBLE);
    }

    protected void initControlBtnHandler() {
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
                controlBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
            } else {
                setMenuVisible(true);
                sideBarMenuVP.setVisibility(View.VISIBLE);
                navContactWithPassenger.setVisibility(View.INVISIBLE);
                controlBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
            }
            controlBtn.startAnimation(anim);
        });
    }

    protected void initLogOutButtonHandler() {
        logOutBtn = activity.findViewById(R.id.map_log_out);
        logOutBtn.setOnClickListener(v -> {
            View.OnClickListener positiveButtonListener = (view) -> {
                DialogAskSomething.alertDialog.dismiss();
                stopTaxiService();
                FirebaseAuth.getInstance().signOut();

                if (!TextUtils.isEmpty(adminUsername)) {
                    try {
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(adminUsername, adminPassword)
                                .addOnCompleteListener(activity, task -> {
                                    if (task.isSuccessful()) {
                                        activity.finish();
                                        Intent intent = new Intent(activity, MainActivity.class);
                                        intent.putExtra("profile_name", "ADMIN");
                                        activity.startActivity(intent);
                                    } else startSignInActivity();
                                }).addOnFailureListener(e -> {
                            e.printStackTrace();
                            startSignInActivity();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        startSignInActivity();
                    }
                } else startSignInActivity();
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

    private void startSignInActivity() {
        activity.finish();
        activity.startActivity(new Intent(activity, SignIn.class));
    }

    private void initActivateAccountHandler() {
        activateAccount = activity.findViewById(R.id.activateAccount);
        activateAccount.setOnClickListener(v -> databaseReferenceTaxi.child("active")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        active = snapshot.getValue(Boolean.class);
                        if (active == null) active = false;

                        String message;
                        if (active) message = "Hesab DEAKTİV edilsinmi?";
                        else message = "Hesab AKTİV edilsinmi?";

                        View.OnClickListener positiveButtonListener = (view) -> {
                            DialogAskSomething.alertDialog.dismiss();
                            if (active) {
                                stopTaxiService();
                                if (TaxiForegroundService.haveOrder) {
                                    resetMarkers();

                                    stopTaxiService();
                                    timerForNewOrder.cancel();

                                    cancelOrderBtn.setVisibility(View.GONE);
                                    contactWithPassengerBtn.setVisibility(View.GONE);
                                    navContactWithPassenger.setVisibility(View.GONE);
                                    statusBtn.setVisibility(View.GONE);
                                    navNewOrder.setVisibility(View.INVISIBLE);
                                    navTaxiInfo.setVisibility(View.VISIBLE);
                                }
                                activity.runOnUiThread(new TimerTask() {
                                    @Override
                                    public void run() {
                                        navTaxiInfo.setBackgroundColor(ContextCompat.getColor(activity,
                                                R.color.nav_taxi_info_color_deactive));
                                        textViewTaxiInfoNavActiveness.setText(R.string.activate);
                                    }
                                });
                            } else {
                                startTimerForNewOrder();
                                startTaxiService();
                                activity.runOnUiThread(new TimerTask() {
                                    @Override
                                    public void run() {
                                        navTaxiInfo.setBackgroundColor(ContextCompat.getColor(activity,
                                                R.color.nav_taxi_info_color_active));
                                        textViewTaxiInfoNavActiveness.setText(R.string.deactivate);
                                    }
                                });
                            }
                        };
                        View.OnClickListener neutralButtonListener = (view) -> {
                            DialogAskSomething.alertDialog.dismiss();
                        };
                        DialogAskSomething dialogAskSomething =
                                new DialogAskSomething(message,
                                        activity.getString(R.string.yes), "", activity.getString(R.string.no),
                                        positiveButtonListener, null, neutralButtonListener, true);
                        dialogAskSomething.show(activity);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }));
        textViewTaxiInfoNavActiveness.setOnClickListener(v -> activateAccount.callOnClick());
    }

    private void startTaxiService() {
        Intent serviceIntent = new Intent(activity, TaxiForegroundService.class);
        ContextCompat.startForegroundService(activity, serviceIntent);
    }

    private void stopTaxiService() {
        activity.stopService(new Intent(activity, TaxiForegroundService.class));
    }

    private void initCallAdminHandler() {
        callAdmin = activity.findViewById(R.id.callAdmin);
        callAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:0777100001"));
            activity.startActivity(intent);
        });
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
     * active directions, and directions view
     */
    private void activateDirections() {
        RecyclerView instructionsRV;
        RecyclerView.Adapter<?> instructionsAdapter;
        RecyclerView.LayoutManager instructionsLayoutManager;

        instructionsRV = activity.findViewById(R.id.nav_instruction_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        instructionsRV.setHasFixedSize(true);

        // use a linear layout manager
        instructionsLayoutManager = new LinearLayoutManager(activity);
        instructionsRV.setLayoutManager(instructionsLayoutManager);

        // specify an adapter (see also next example)
        instructionsAdapter = new InstructionAdapter(Navigator.getNavigator().getGhResponse().getInstructions());
        instructionsRV.setAdapter(instructionsAdapter);
//        initNavListView();
    }

    /**
     * handler clicks on nav button
     */
    public void initNavBtnHandler() {
        navigationBtn.setOnClickListener(v -> {
            if (!Navigator.getNavigator().isOn()) {
                if (TaxiForegroundService.orderStatus == 3)
                    createOrderPath(true, true, false);
                else createOrderPath(true, false, true);
                navigationBtn.setVisibility(View.GONE);
            }
        });
    }

    public void initCancelBtnHandler() {
        cancelOrderBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Sifariş ləğv edilsinmi?")
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        // stop!
                        resetMarkers();

                        databaseReferenceOrders
                                .child("ORDERS/ACTIVE/" + TaxiForegroundService.orderName + "/type").setValue(1);

                        TaxiForegroundService.reset();
                        stopTaxiService();
                        startTaxiService();
                        startTimerForNewOrder();
                        cancelOrderBtn.setVisibility(View.GONE);
                        contactWithPassengerBtn.setVisibility(View.GONE);
                        navContactWithPassenger.setVisibility(View.GONE);
                        statusBtn.setVisibility(View.GONE);
                        navTaxiInfo.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                    }).setNegativeButton(R.string.cancel, (dialog, id) -> {
                // User cancelled the dialog
                dialog.dismiss();
            });
            builder.create().show();
        });
    }

    //---------------------------------------
    public Map.UpdateListener createUpdateListener() {
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

    public void onPressLocationEndPoint(GeoPoint latLong) {
        tabAction = TabAction.EndPoint;
        onPressLocation(latLong);
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

            if (Variable.getVariable().isDirectionsON()) {
                MapHandlerTaxi.getMapHandler().setNeedPathCal(true);
                // Waiting for calculating
            }
            return true;
        }
        return false;
    }

    /**
     * navigation list view
     * <p>
     * make nav list view control button ready to use
     */
    protected void initNavListView() {
        fillNavListSummaryValues();

        final ImageButton clearBtn, stopBtn, stopNavBtn;
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
                        Navigator.getNavigator().setNaviStart(activity, false);

                        navTopVP.setVisibility(View.INVISIBLE);
                        sideBarVP.setVisibility(View.VISIBLE);
                        navigationBtn.setVisibility(View.VISIBLE);
                        navTaxiInfo.setVisibility(View.INVISIBLE);
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

        clearBtn.setOnClickListener(v -> sideBarVP.setVisibility(View.VISIBLE));
        stopNavBtn.setOnClickListener(v -> stopBtn.callOnClick());
    }

    /**
     * fill up values for nav list summary
     */
    private void fillNavListSummaryValues() {
        TextView from, to, distance, time;
        from = activity.findViewById(R.id.nav_instruction_list_summary_from_tv);
        to = activity.findViewById(R.id.nav_instruction_list_summary_to_tv);
        distance = activity.findViewById(R.id.nav_instruction_list_summary_distance_tv);
        time = activity.findViewById(R.id.nav_instruction_list_summary_time_tv);

        from.setText(Destination.getDestination().getStartPointToString());
        to.setText(Destination.getDestination().getEndPointToString());
        distance.setText(Navigator.getNavigator().getDistanceByStringWithUnit());
        time.setText(Navigator.getNavigator().getTime());
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

    @Override
    public void onNaviStart(boolean on) {
        if (on) {
            navTopVP.setVisibility(View.VISIBLE);
            navTaxiInfo.setVisibility(View.INVISIBLE);
        } else {
            navTopVP.setVisibility(View.INVISIBLE);
        }
    }
}
