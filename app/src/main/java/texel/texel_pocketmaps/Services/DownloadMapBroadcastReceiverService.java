package texel.texel_pocketmaps.Services;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.downloader.MapDownloadUnzip;
import texel.texel_pocketmaps.model.MyMap;


public class DownloadMapBroadcastReceiverService extends Service {
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static BroadcastReceiver brDownloadReceiver;
    public static long enqueueId;
    public static MyMap myMap;
    public static Activity activity;
    public static MapDownloadUnzip.StatusUpdate stUpdate;

    public static void clearDlFile(MyMap myMap) {
        File destFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlMapFile);
        if (destFile.exists()) {
            destFile.delete();
        }
        destFile = MyMap.getMapFile(myMap, MyMap.MapFileType.DlIdFile);
        if (destFile.exists()) {
            destFile.delete();
        }
    }

    public static void setStatus(int status) {
        if (Environment.getExternalStorageState().equalsIgnoreCase("mounted"))//Check if Device Storage is present
        {
            try {
                File myTxt = MyMap.getMapFile(myMap, MyMap.MapFileType.MyFile);

                FileWriter writer = new FileWriter(myTxt);
                writer.write(status); //Writing the text
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        registerScreenOffReceiver();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(brDownloadReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_FOREGROUND_SERVICE.equals(action)) stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerScreenOffReceiver() {
        brDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    int status = MapDownloadUnzip.getDownloadStatus(context, enqueueId);
                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            MapDownloadUnzip.unzipBg(activity, myMap, stUpdate);
                            setStatus(1);
                            break;
                        case -1:
                            myMap.setStatus(MyMap.DlStatus.On_server);
                            clearDlFile(myMap);
                            setStatus(0);
                            break;
                        case DownloadManager.STATUS_FAILED:
                            myMap.setStatus(MyMap.DlStatus.Error);
                            clearDlFile(myMap);
                            setStatus(0);
                            break;
                        default:
                            setStatus(0);
                    }
                    stopSelf();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(brDownloadReceiver, filter);

        startForegroundService();
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        } else {
            // Create notification builder.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            // Make notification show big text.
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("Xəritə yüklənir...");
            bigTextStyle.bigText("Xəritə yüklənənədək gözləyin.");
            builder.setStyle(bigTextStyle);
            builder.setSmallIcon(R.mipmap.download_icon_green_foreground);
            builder.setPriority(Notification.PRIORITY_MAX);

            setCancelAction(builder);

            Notification notification = builder.build();
            startForeground(1, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel chan = new NotificationChannel("TAG_FOREGROUND_SERVICE", "TAG_FOREGROUND_SERVICE", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "TAG_FOREGROUND_SERVICE");
        notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_cloud_download_white_24dp)
                .setColor(getResources().getColor(R.color.accent))
                .setContentTitle("Xəritə yüklənir...")
                .setContentText("Xəritə yüklənənədək gözləyin.")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE);

        setCancelAction(notificationBuilder);

        Notification notification = notificationBuilder.build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build());
        startForeground(1, notification);
    }

    private void setCancelAction(NotificationCompat.Builder notificationBuilder) {
        Intent playIntent = new Intent();
        playIntent.setAction(ACTION_STOP_FOREGROUND_SERVICE);
        PendingIntent pendingPrevIntent = PendingIntent.getService(this, 0, playIntent, 0);
        NotificationCompat.Action prevAction = new NotificationCompat.Action(0, "Ləğv et", pendingPrevIntent);
        notificationBuilder.addAction(prevAction);
    }
}