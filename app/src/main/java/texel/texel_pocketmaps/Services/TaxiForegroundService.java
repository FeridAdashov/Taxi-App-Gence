package texel.texel_pocketmaps.Services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.activities.HelperActivities.CloseTaxiServiceDialogActivity;
import texel.texel_pocketmaps.activities.MapActivity;
import texel.texel_pocketmaps.activities.SignActivities.SignIn;
import texel.texel_pocketmaps.map.Navigator;


public class TaxiForegroundService extends Service {

    private static final String TAG = "MyLocationService";
    private static final int LOCATION_INTERVAL = 10000;
    private static final float LOCATION_DISTANCE = 30f;
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static Timer timerForOrderSeconds, timerForUpdateUserInfo;
    public static Double orderStartLat, orderStartLon, orderEndLat, orderEndLon;
    public static boolean haveOrder = false, isServiceActive = false;
    public static long activeSeconds = 0;
    public static int orderStatus;
    public static String orderName = "";
    LocationListener[] mLocationListeners = new LocationListener[]{new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.PASSIVE_PROVIDER), new LocationListener(LocationManager.NETWORK_PROVIDER)};
    private Location mLastLocation;
    private LocationManager mLocationManager = null;
    private DatabaseReference databaseReference;
    private ValueEventListener listener;

    public TaxiForegroundService() {
    }

    public static void cancelTimers() {
        if (timerForOrderSeconds != null) timerForOrderSeconds.cancel();
        if (timerForUpdateUserInfo != null) timerForUpdateUserInfo.cancel();
    }

    public static void reset() {
        Navigator.getNavigator().setOn(false);
        haveOrder = false;
        activeSeconds = 0;
        orderEndLon = 0.;
        orderEndLat = 0.;
        orderStartLon = 0.;
        orderStartLat = 0.;
        orderName = "";
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        databaseReference = DatabaseFunctions.getDatabases(getApplicationContext())
                .get(0)
                .child("USERS/TAXI/" + FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0]);
        isServiceActive = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (haveOrder) removeOrder(orderName);

        databaseReference.child("active").setValue(false);
        databaseReference.child("orderName").setValue("");
        databaseReference.removeEventListener(listener);
        cancelTimers();
        reset();

        if (mLocationManager != null)
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex);
                }
            }
    }

    private void removeOrder(String order_name) {
        if (!TextUtils.isEmpty(order_name))
            DatabaseFunctions.getDatabases(getApplicationContext()).get(1).child("ORDERS/ACTIVE/" + order_name + "/type")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer type = snapshot.getValue(Integer.class);
                            if (type != null) snapshot.getRef().setValue(1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService("Sifariş axtarılır...", "Yeni sifariş təyin edilənədək gözləyin...", "");

        initializeLocationManager();
        try {
            for (LocationListener listener : mLocationListeners)
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_INTERVAL,
                        LOCATION_DISTANCE,
                        listener
                );
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }

        databaseReference.child("active").setValue(true);
        listener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    String lastActiveTime = dataSnapshot.child("lastActiveTime").getValue(String.class);
                    if (!TextUtils.isEmpty(lastActiveTime) && MyHelperFunctions.getDifferentTimeInSeconds(MyHelperFunctions.dateFormatter.parse(lastActiveTime)) < 110) {
                        String orderNameNew = dataSnapshot.child("orderName").getValue(String.class);
                        if (!TextUtils.isEmpty(orderNameNew) && !orderNameNew.equals(orderName)) {
                            orderName = orderNameNew;
                            startCounting();
                        }
                    } else orderName = "";
                } catch (Exception e) {
                    Log.d("AAAAA", e.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        startLocationTimer();
        return START_STICKY;
    }

    private void startLocationTimer() {
        timerForUpdateUserInfo = new Timer();
        timerForUpdateUserInfo.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (MapActivity.getmCurrentLocation() != null
                            && MapActivity.getmCurrentLocation().getLatitude() != 0
                            && MapActivity.getmCurrentLocation().getLongitude() != 0)
                        mLastLocation = MapActivity.getmCurrentLocation();
                    else {
                        if (ActivityCompat.checkSelfPermission(getBaseContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getBaseContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                            return;
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, LOCATION_DISTANCE, mLocationListeners[0]);
                    }

                    databaseReference.child("LOCATION/lat").setValue(mLastLocation.getLatitude());
                    databaseReference.child("LOCATION/lon").setValue(mLastLocation.getLongitude());
                    databaseReference.child("lastActiveTime").setValue(MyHelperFunctions.dateFormatter.format(new Date()));
                } catch (Exception e) {
                    Log.d("AAAAAAAA", e.toString());
                }
            }
        }, 0, 10000);
    }

    public void startCounting() {
        DatabaseFunctions.getDatabases(getApplicationContext()).get(1).child("ORDERS/ACTIVE/" + orderName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    orderStartLat = snapshot.child("p1Lat").getValue(Double.class);
                    orderStartLon = snapshot.child("p1Lon").getValue(Double.class);
                    orderEndLat = snapshot.child("p2Lat").getValue(Double.class);
                    orderEndLon = snapshot.child("p2Lon").getValue(Double.class);

                    activeSeconds = /*121*/20 - MyHelperFunctions.getDifferentTimeInSeconds(
                            MyHelperFunctions.parseStringToDate(snapshot.child("taxiTimer").getValue(String.class)));
                    haveOrder = true;
                    startForegroundService("Yeni Sifaris", "Sifarisi Qebul Edin!", "Göstər");

                    if (timerForOrderSeconds != null) timerForOrderSeconds.cancel();
                    timerForOrderSeconds = new Timer();
                    timerForOrderSeconds.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (--activeSeconds <= 0) {
                                Log.d("AAAA", "Vaxt bitdi, Sifaris legv edildi!!!");
                                removeOrder(orderName);
                                reset();
                                startForegroundService("Sifariş axtarılır...", "Yeni sifariş təyin edilənədək gözləyin...", "");
                                databaseReference.child("orderName").setValue("");
                                timerForOrderSeconds.cancel();
                            }
                        }
                    }, 0, 1000);
                } catch (Exception e) {
                    Log.d("AAAA", e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    /* Used to build and start foreground service. */
    public void startForegroundService(String title, String message, String positive_button_text) {
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.notification_sound);
        mp.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(title, message, positive_button_text);
        } else {
            // Create notification builder.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            // Make notification show big text.
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(title);
            bigTextStyle.bigText(message);
            // Set big text style.
            builder.setStyle(bigTextStyle);

            builder.setWhen(System.currentTimeMillis());
            builder.setSmallIcon(R.mipmap.taxi__notification_icon_foreground);
            builder.setPriority(Notification.PRIORITY_MAX);

            addActionsToButtons(builder, positive_button_text);

            Notification notification = builder.build();
            startForeground(1, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String title, String message, String positive_button_text) {
        NotificationChannel chan = new NotificationChannel(TAG_FOREGROUND_SERVICE, TAG_FOREGROUND_SERVICE, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, TAG_FOREGROUND_SERVICE);
        notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_taxi_orange)
                .setColor(getResources().getColor(R.color.accent))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE);

        addActionsToButtons(notificationBuilder, positive_button_text);

        Notification notification = notificationBuilder.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build());
        startForeground(1, notification);
    }

    private void addActionsToButtons(NotificationCompat.Builder notificationBuilder, String positive_button_text) {
        Intent playIntent = new Intent(this, CloseTaxiServiceDialogActivity.class);
        PendingIntent pendingPlayIntentNegative = PendingIntent.getActivity(this, 0, playIntent, 0);
        notificationBuilder.addAction(0, getString(R.string.m_cancel), pendingPlayIntentNegative);

        if (!positive_button_text.equals("")) {
            playIntent = new Intent(this, SignIn.class);
            PendingIntent pendingPlayIntentPositive = PendingIntent.getActivity(this, 1, playIntent, 0);
            notificationBuilder.addAction(0, positive_button_text, pendingPlayIntentPositive);
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager - LOCATION_INTERVAL: " + LOCATION_INTERVAL + " LOCATION_DISTANCE: " + LOCATION_DISTANCE);
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener {
        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
}