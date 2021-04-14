package texel.texel_pocketmaps.admin_pages.new_orders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Order;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.admin_pages.taxis_for_order.TaxisForOrderActivity;
import texel.texel_pocketmaps.map.MapHandler;

public class RecyclerViewAdapterNewOrders extends RecyclerView.Adapter<RecyclerViewAdapterNewOrders.MyViewHolder> {
    private final HashMap<String, TimeTextView> timeTextViewList = new HashMap<>();
    private final ArrayList<Order> orderArrayList;
    private final ArrayList<String> orderNameArrayList;
    private final Activity activity;
    private Timer timer = new Timer();

    public RecyclerViewAdapterNewOrders(Activity activity,
                                        ArrayList<String> orderNameArrayList,
                                        ArrayList<Order> orderArrayList) {
        this.orderNameArrayList = orderNameArrayList;
        this.orderArrayList = orderArrayList;
        this.activity = activity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_new_orders, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        if (position == 0) {
            if (timer != null) timer.cancel();
            timeTextViewList.clear();
        }

        String order_name = orderNameArrayList.get(position);
        holder.name.setText(orderArrayList.get(position).getPassengerName());

        MapHandler.getMapHandler().calcPath(
                orderArrayList.get(position).getP1Lat(),
                orderArrayList.get(position).getP1Lon(),
                orderArrayList.get(position).getP2Lat(),
                orderArrayList.get(position).getP2Lon(),
                activity,
                holder.distance, null, holder.money);

        try {
            timeTextViewList.put(order_name,
                    new TimeTextView(holder.time, MyHelperFunctions.dateFormatter.parse(order_name.split(" ")[0])));
        } catch (Exception e) {
            Log.d("AAAAAA", e.toString());
        }

        holder.buttonDeleteOrder.setOnClickListener(v -> openDialogForDelete(order_name));
        holder.buttonDirectOrder.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TaxisForOrderActivity.class);
            intent.putExtra("orderName", order_name);
            activity.startActivity(intent);
        });

        if (position == orderArrayList.size() - 1) startTimeTextViewTimer();
    }

    @Override
    public int getItemCount() {
        return orderArrayList.size();
    }

    private void startTimeTextViewTimer() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (String orderName : orderNameArrayList) {
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                TimeTextView tt = timeTextViewList.get(orderName);
                                if (tt == null) return;

                                long seconds = MyHelperFunctions.getDifferentTimeInSeconds(tt.orderDate);
                                tt.textView.setText(MyHelperFunctions.convertSecondsToString(seconds));

                                if (MyHelperFunctions.isBigDifference(tt.orderDate, 60 * 10))
                                    deleteOrderByPosition(orderName);
                            } catch (Exception e) {
                                Log.d("AAAAA " + orderName + " " + timeTextViewList.size(), e.toString());
                            }
                        }
                    });
                }
            }
        }, 0, 1000);
    }

    private void deleteOrderByPosition(String orderName) {
        DatabaseFunctions.getDatabases(activity).get(1).child("ORDERS/ACTIVE/" + orderName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        DatabaseFunctions.getDatabases(activity).get(1)
                                .child("ORDERS/DELETED/" + new SimpleDateFormat("dd_MM_yyyy").format(new Date())
                                        + "/" + orderName).setValue(snapshot.getValue());
                        snapshot.getRef().removeValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void openDialogForDelete(String orderName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Sifariş silinsinmi?");
        builder.setNeutralButton(R.string.m_cancel, null);
        builder.setNegativeButton(R.string.yes, (dialog, which) -> deleteOrderByPosition(orderName));
        builder.create().show();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView distance;
        private final TextView money;
        private final TextView time;
        private final Button buttonDeleteOrder;
        private final Button buttonDirectOrder;

        MyViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.card_view_name);
            distance = itemView.findViewById(R.id.card_view_distance);
            money = itemView.findViewById(R.id.card_view_money);
            time = itemView.findViewById(R.id.card_view_time);
            buttonDeleteOrder = itemView.findViewById(R.id.buttonDeleteOrder);
            buttonDirectOrder = itemView.findViewById(R.id.buttonDirectOrder);
        }
    }

    static class TimeTextView {
        public TextView textView;
        Date orderDate;

        public TimeTextView(TextView textView, Date orderDate) {
            this.textView = textView;
            this.orderDate = orderDate;
        }
    }
}