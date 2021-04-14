package texel.texel_pocketmaps.admin_pages.searching_taxi;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.SearchingTaxi;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;

public class RecyclerViewAdapterSearchingTaxis extends RecyclerView.Adapter<RecyclerViewAdapterSearchingTaxis.MyViewHolder> {
    private final HashMap<String, TimeTextView> timeTextViewList = new HashMap<>();
    private final ArrayList<SearchingTaxi> searchingTaxiArrayList;
    private final ArrayList<String> orderArrayList;
    private final Activity activity;
    public Timer timer = new Timer();


    public RecyclerViewAdapterSearchingTaxis(Activity activity,
                                             ArrayList<String> orderArrayList,
                                             ArrayList<SearchingTaxi> searchingTaxiArrayList) {
        this.orderArrayList = orderArrayList;
        this.searchingTaxiArrayList = searchingTaxiArrayList;
        this.activity = activity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_searching_taxis, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        if (position == 0) {
            cancelTimer();
            timeTextViewList.clear();
        }

        String order_name = orderArrayList.get(position);
        holder.passengerName.setText(searchingTaxiArrayList.get(position).passenger);
        holder.taxiName.setText(searchingTaxiArrayList.get(position).taxi);

        timeTextViewList.put(order_name, new TimeTextView(holder.time, searchingTaxiArrayList.get(position).time));

        holder.buttonDirectOrder.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Sifariş başqa taksiyə yönəldilsinmi?");
            builder.setNegativeButton(R.string.m_cancel, null);
            builder.create().show();
        });

        if (position == searchingTaxiArrayList.size() - 1) startTimeTextViewTimer();
    }

    @Override
    public int getItemCount() {
        return searchingTaxiArrayList.size();
    }

    private void startTimeTextViewTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (String orderName : orderArrayList) {
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            TimeTextView tt = timeTextViewList.get(orderName);
                            if (tt == null) return;

                            long seconds = MyHelperFunctions.getDifferentTimeInSeconds(tt.orderDate);
                            tt.textView.setText(MyHelperFunctions.convertSecondsToString(seconds));

                            if (seconds > 20/*120*/) {
                                DatabaseFunctions.getDatabases(activity).get(1)
                                        .child("ORDERS/ACTIVE/" + orderName + "/type").setValue(1);
                            }
                        }
                    });
                }
            }
        }, 0, 1000);
    }

    private void cancelTimer() {
        if (timer != null) {
            Log.d("AAAAAAA", "Timer Canceled");
            timer.cancel();
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView passengerName;
        private final TextView taxiName;
        private final TextView time;
        private final Button buttonDirectOrder;

        MyViewHolder(View itemView) {
            super(itemView);

            passengerName = itemView.findViewById(R.id.card_view_passengerName);
            taxiName = itemView.findViewById(R.id.card_view_taxiName);
            time = itemView.findViewById(R.id.card_view_time);
            buttonDirectOrder = itemView.findViewById(R.id.cardButtonDirect);
        }
    }

    static class TimeTextView {
        public TextView textView;
        public Date orderDate;

        public TimeTextView(TextView textView, Date orderDate) {
            this.textView = textView;
            this.orderDate = orderDate;
        }
    }
}