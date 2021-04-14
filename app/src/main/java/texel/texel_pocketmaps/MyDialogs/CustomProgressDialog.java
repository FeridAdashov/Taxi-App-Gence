package texel.texel_pocketmaps.MyDialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;

public class CustomProgressDialog {
    private final Activity activity;
    private final AlertDialog.Builder builder;
    private ProgressBar progressBar;
    private TextView textViewDots, textViewMessage;
    private AlertDialog alertDialog;
    private Timer timer = new Timer();
    private String message;
    private int progress = 0;

    public CustomProgressDialog(Activity activity, String message) {
        builder = new AlertDialog.Builder(activity);
        this.activity = activity;
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCancelable(boolean isCancelable) {
        builder.setCancelable(isCancelable);
    }

    public void show() {
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.progress_view, null);
        textViewMessage = view.findViewById(R.id.textViewMessage);
        textViewMessage.setText(message);
        textViewDots = view.findViewById(R.id.textViewDots);
        progressBar = view.findViewById(R.id.progressBar);
        builder.setView(view);

        alertDialog = builder.create();
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        startSplashTimer();
    }

    public void dismiss() {
        alertDialog.dismiss();
        timer.cancel();
    }

    private void startSplashTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                progressBar.setProgress(progress++);
                if (progress == 100) progress = 0;
                activity.runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        if (progress % 30 < 10) textViewDots.setText(".");
                        else if (progress % 30 < 20) textViewDots.setText("..");
                        else textViewDots.setText("...");

                        textViewMessage.setText(message);
                    }
                });
            }
        }, 0, 30);
    }
}
