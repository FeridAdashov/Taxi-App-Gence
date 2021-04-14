package texel.texel_pocketmaps.activities.SignActivities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomHorizontalProgressBar;
import texel.texel_pocketmaps.activities.MainActivity;
import texel.texel_pocketmaps.util.Variable;

public class SignIn extends AppCompatActivity implements View.OnClickListener {

    public static final String TIME_SERVER = "time-a.nist.gov";
    private static String rootName;
    private final int TAG_WRITE_EXTERNAL_STORAGE = 1;
    private final int TAG_READ_EXTERNAL_STORAGE = 2;
    private final int TAG_ACCESS_FINE_LOCATION = 3;
    private final int TAG_ACCESS_COARSE_LOCATION = 4;
    private final int TAG_ACCESS_BACKGROUND_LOCATION = 5;
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:SS");
    private EditText editTextEmail;
    private EditText editTextPassword;
    private TextView textViewSignUp, buttonSignIn;
    private CustomHorizontalProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private Activity activity;
    private Timer timer = new Timer();
    private boolean active = true;
    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                NTPUDPClient timeClient = new NTPUDPClient();
                InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
                TimeInfo timeInfo = timeClient.getTime(inetAddress);
                long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time

                Date date_1 = formatter.parse(formatter.format(new Date(returnTime)));
                Date date_2 = formatter.parse(formatter.format(new Date()));
                differenceBetweenGlobalAndLocalClock(date_1, date_2);

//                int day_of_month = calendar.get(Calendar.DAY_OF_MONTH);
//                if(day_of_month <= 6) Variable.getVariable().taxi_active_database_index = 2;
//                else if(day_of_month <= 12) Variable.getVariable().taxi_active_database_index = 3;
//                else if(day_of_month <= 18) Variable.getVariable().taxi_active_database_index = 4;
//                else if(day_of_month <= 24) Variable.getVariable().taxi_active_database_index = 5;
//                else Variable.getVariable().taxi_active_database_index = 6;
                Variable.getVariable().taxi_active_database_index = 0;
            } catch (Exception e) {
                Log.d("AAAAA", e.toString());
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        changeProgressBarVisibility(false);
                        Toast.makeText(activity, R.string.error_check_internet, Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                });
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        activity = this;

        buttonSignIn = findViewById(R.id.buttonSignIn);
        progressBar = findViewById(R.id.progress);
        changeProgressBarVisibility(true);

        thread.start();
    }

    public void differenceBetweenGlobalAndLocalClock(final Date startDate, final Date endDate) {
        //seconds
        final long different = (endDate.getTime() - startDate.getTime()) / 1000;

        activity.runOnUiThread(new TimerTask() {
            @Override
            public void run() {
                if (Math.abs(different) > 90) { // 1 MINUTE
                    changeProgressBarVisibility(false);
                    Toast.makeText(activity, "Telefonunuzda SAAT SƏHVDİR! Saat: " + formatter.format(startDate) + " olmalıdır!", Toast.LENGTH_LONG).show();
                    finish();
                } else
                    reLoadSeed();
            }
        });
    }

    private void loadDatabases() {
        if (firebaseAuth.getCurrentUser() != null) {
            changeProgressBarVisibility(true);

            rootName = firebaseAuth.getCurrentUser().getEmail().split("@")[0];
            databaseReference = DatabaseFunctions.getDatabases(this).get(0);
            databaseReference.child("DatabaseInformation/version").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer version = dataSnapshot.getValue(Integer.class);
                    if (version != null && version == 1)
                        goToProfile();
                    else {
                        changeProgressBarVisibility(false);
                        firebaseAuth.signOut();
                        Toast.makeText(getBaseContext(), "Proqramı Güncəlləyin!", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    changeProgressBarVisibility(false);
                }
            });
        } else changeProgressBarVisibility(false);
    }

    private void reLoadSeed() {
        buttonSignIn.setOnClickListener(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);

        textViewSignUp = findViewById(R.id.textViewSignUp);
        textViewSignUp.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        checkMyPermissions();
    }

    private void checkMyPermissions() {
        int permissionWriteExternal = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionReadExternal = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionWriteExternal != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, TAG_WRITE_EXTERNAL_STORAGE);
            return;
        }
        if (permissionReadExternal != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, TAG_READ_EXTERNAL_STORAGE);
            return;
        }
        if (!getLocationPermission()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkDeviceNeedPermission()) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle("Batareya Qənaət Rejimi")
                        .setMessage("Məkan (GPS) paylaşımı üçün:\n" +
                                "1. parametrlərdən batareya qənaət rejimini tapın\n" +
                                "2. " + getString(R.string.app_name) + " proqramını tapın\n" +
                                "3. Məhdudiyyət Yoxdur rejimini seçin")
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            active = false;

                            timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (active) {
                                        activity.runOnUiThread(new TimerTask() {
                                            @Override
                                            public void run() {
                                                changeProgressBarVisibility(true);
                                                checkMyPermissions();
                                            }
                                        });
                                        timer.cancel();
                                    }
                                }
                            }, 0, 1000);

                            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            i.addCategory(Intent.CATEGORY_DEFAULT);
                            i.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        })
                        .setNegativeButton(R.string.m_cancel, (dialog, which) -> finish())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .show();
            } else loadDatabases();
        } else loadDatabases();
    }

    private boolean checkDeviceNeedPermission() {
        String manufacturer = Build.MANUFACTURER;
        return "xiaomi".equalsIgnoreCase(manufacturer)
                || "oppo".equalsIgnoreCase(manufacturer)
                || "vivo".equalsIgnoreCase(manufacturer)
                || "Letv".equalsIgnoreCase(manufacturer)
                || "Honor".equalsIgnoreCase(manufacturer);
    }

    private boolean getLocationPermission() {
        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, TAG_ACCESS_FINE_LOCATION);
                return false;
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, TAG_ACCESS_COARSE_LOCATION);
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, TAG_ACCESS_BACKGROUND_LOCATION);
            return false;
        }
        return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Məkanı (GPS) aktivləşdirin!!!")
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    changeProgressBarVisibility(false);
                    active = false;

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (active) {
                                activity.runOnUiThread(new TimerTask() {
                                    @Override
                                    public void run() {
                                        changeProgressBarVisibility(true);
                                        checkMyPermissions();
                                    }
                                });
                                timer.cancel();
                            }
                        }
                    }, 0, 1000);
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton(R.string.m_cancel, (dialog, id) -> {
                    dialog.cancel();
                    finish();
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case TAG_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    changeProgressBarVisibility(true);
                    checkMyPermissions();
                } else {
                    Toast.makeText(this, getString(R.string.write_permission_denied), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
            case TAG_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    changeProgressBarVisibility(true);
                    checkMyPermissions();
                } else {
                    Toast.makeText(this, getString(R.string.read_permission_denied), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
            case TAG_ACCESS_FINE_LOCATION:
            case TAG_ACCESS_COARSE_LOCATION:
            case TAG_ACCESS_BACKGROUND_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    changeProgressBarVisibility(true);
                    checkMyPermissions();
                } else {
                    Toast.makeText(this, "Məkana icazə verilmədi", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void goToProfile() {
        databaseReference.child("USERS").addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                changeProgressBarVisibility(false);
                try {
                    String user;
                    if (dataSnapshot.child("ADMIN/" + rootName).exists())
                        user = "ADMIN";
                    else if (dataSnapshot.child("PASSENGER/" + rootName).exists())
                        user = "PASSENGER";
                    else if (dataSnapshot.child("TAXI/" + rootName).exists())
                        user = "TAXI";
                    else return;

                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    intent.putExtra("profile_name", user);
                    finish();
                    startActivity(intent);
                } catch (Exception e) {
                    changeProgressBarVisibility(false);
                    firebaseAuth.signOut();
                    Toast.makeText(getBaseContext(), R.string.maybe_profile_data_wrong, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                changeProgressBarVisibility(false);
                Toast.makeText(getBaseContext(), R.string.error_check_internet, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void userLogIn() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.enter_email, Toast.LENGTH_SHORT).show();
            return;
        } else email += "@mail.ru";

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.enter_password, Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        changeProgressBarVisibility(true);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                loadDatabases();
            } else {
                changeProgressBarVisibility(false);
                Toast.makeText(getApplicationContext(), R.string.incorrect_email, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonSignIn) {
            userLogIn();
        }

        if (view == textViewSignUp) {
            startActivity(new Intent(this, SignUp.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        active = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    private void changeProgressBarVisibility(boolean b) {
        progressBar.setVisibility(b ? View.VISIBLE : View.GONE);
        buttonSignIn.setEnabled(!b);
    }
}
