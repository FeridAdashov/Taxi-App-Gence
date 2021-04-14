package texel.texel_pocketmaps.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.activities.HelperActivities.ClosePassengerServiceDialogActivity;
import texel.texel_pocketmaps.activities.HelperActivities.PassengerTaxiChatActivity;


public class PassengerForegroundService extends Service {

    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";

    public static String orderName = "";
    public static boolean isActive = false;
    public static int orderNotificationStep = 1;   //Prevent open same service
    private Timer timer;
    private DatabaseReference databaseReference;

    public PassengerForegroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isActive) {
            isActive = false;
            databaseReference.removeValue(); //Remove Order
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isActive = true;
        orderName = intent.getStringExtra("order_name");

        databaseReference = DatabaseFunctions.getDatabases(getApplicationContext())
                .get(1)
                .child("ORDERS/ACTIVE/" + orderName);

        startForegroundService("Taksi axtarılır...", "Taksi təyin edilənədək gözləyin...", false, "");

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                databaseReference.child("type").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Integer type = dataSnapshot.getValue(Integer.class);
                        if (type != null) {
                            switch (type) {
                                case 3:
                                    if (orderNotificationStep < 3) {
                                        orderNotificationStep = 3;
                                        startForegroundService("Taksi təyin edildi!", "Taksi çatanadək gözləyin və ya əlaqə saxlayın!", true, "Taksi ilə Əlaqə");
                                    }
                                    break;

                                case 4:
                                    if (orderNotificationStep < 4) {
                                        orderNotificationStep = 4;
                                        startForegroundService("Taksi çatmışdır!", "Qarşılayın və ya əlaqə saxlayın!", true, "Taksi ilə Əlaqə");
                                    }
                                    break;

                                case 5:
                                    if (orderNotificationStep < 5) {
                                        orderNotificationStep = 5;
                                        timer.cancel();
                                        isActive = false;
                                        stopForegroundService();
                                    }
                                    break;
                            }
                        } else {
                            timer.cancel();
                            isActive = false;
                            stopForegroundService();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }, 0, 5000);

        return super.onStartCommand(intent, flags, startId);
    }

    /* Used to build and start foreground service. */
    private void startForegroundService(String title, String message, boolean positive_button_enable, String positive_button_text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(title, message, positive_button_enable, positive_button_text);
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

            addActionsToButtons(builder, positive_button_enable, positive_button_text);

            Notification notification = builder.build();
            startForeground(1, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String title, String message, boolean positive_button_enable, String positive_button_text) {
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

        addActionsToButtons(notificationBuilder, positive_button_enable, positive_button_text);

        Notification notification = notificationBuilder.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build());
        startForeground(1, notification);
    }

    private void addActionsToButtons(NotificationCompat.Builder notificationBuilder, boolean positive_button_enable, String positive_button_text) {
        Intent playIntent = new Intent(this, ClosePassengerServiceDialogActivity.class);
        PendingIntent pendingPlayIntentNegative = PendingIntent.getActivity(this, 0, playIntent, 0);
        notificationBuilder.addAction(0, getString(R.string.m_cancel), pendingPlayIntentNegative);

        if (positive_button_enable) {
            playIntent = new Intent(this, PassengerTaxiChatActivity.class);
            playIntent.putExtra("order_name", orderName);
            PendingIntent pendingPlayIntentPositive = PendingIntent.getActivity(this, 1, playIntent, 0);
            notificationBuilder.addAction(0, positive_button_text, pendingPlayIntentPositive);
        }
    }


    private void stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }
}