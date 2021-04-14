package texel.texel_pocketmaps.activities.HelperActivities;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.Services.TaxiForegroundService;

public class CloseTaxiServiceDialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_taxi_service_dialog);

        TextView textViewMessage = findViewById(R.id.closeMessage);
        textViewMessage.setText(R.string.account_will_close_when_service_shutdown);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenHeight = (int) (metrics.heightPixels * 0.30);

        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, screenHeight);

        Button buttonCloseDialog = findViewById(R.id.buttonCloseDialog);
        Button buttonCloseService = findViewById(R.id.buttonCloseService);

        buttonCloseDialog.setOnClickListener(v -> finish());
        buttonCloseService.setOnClickListener(v -> stopService());
    }

    private void stopService() {
        stopService(new Intent(getApplicationContext(), TaxiForegroundService.class));
        finish();
    }
}
