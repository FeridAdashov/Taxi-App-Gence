package texel.texel_pocketmaps.MyDialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import texel.texel.gencetaxiapp.R;

public class DialogAskSomething {

    public static AlertDialog alertDialog = null;
    private final String message;
    private final String positiveButtonText;
    private final String negativeButtonText;
    private final String neutralButtonText;
    private final boolean cancelable;
    private final View.OnClickListener positiveButtonListener;
    private final View.OnClickListener negativeButtonListener;
    private final View.OnClickListener neutralButtonListener;

    public DialogAskSomething(String message, String positiveButtonText,
                              String negativeButtonText, String neutralButtonText,
                              View.OnClickListener positiveButtonListener,
                              View.OnClickListener negativeButtonListener,
                              View.OnClickListener neutralButtonListener,
                              boolean cancelable) {
        this.message = message;
        this.positiveButtonText = positiveButtonText;
        this.negativeButtonText = negativeButtonText;
        this.neutralButtonText = neutralButtonText;
        this.positiveButtonListener = positiveButtonListener;
        this.negativeButtonListener = negativeButtonListener;
        this.neutralButtonListener = neutralButtonListener;
        this.cancelable = cancelable;
    }

    public void show(final Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ask_dialog_view, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(cancelable);

        TextView messageTextView = (TextView) dialogView.findViewById(R.id.message);
        messageTextView.setText(message);

        Button positive = (Button) dialogView.findViewById(R.id.positive);
        Button negative = (Button) dialogView.findViewById(R.id.negative);
        Button neutral = (Button) dialogView.findViewById(R.id.neutral);

        if (positiveButtonListener != null) {
            positive.setText(positiveButtonText);
            positive.setOnClickListener(positiveButtonListener);
        } else positive.setVisibility(View.GONE);

        if (negativeButtonListener != null) {
            negative.setText(negativeButtonText);
            negative.setOnClickListener(negativeButtonListener);
        } else negative.setVisibility(View.GONE);

        if (neutralButtonListener != null) {
            neutral.setText(neutralButtonText);
            neutral.setOnClickListener(neutralButtonListener);
        } else neutral.setVisibility(View.GONE);

        alertDialog = dialogBuilder.create();
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    public void dismiss() {
        if (alertDialog != null) alertDialog.dismiss();
    }
}
