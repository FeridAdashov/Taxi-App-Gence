package texel.texel_pocketmaps.admin_pages.on_way;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;

public class RecyclerViewAdapterOnWay extends RecyclerView.Adapter<RecyclerViewAdapterOnWay.MyViewHolder> {
    private final ArrayList<String> onWayTaxiNameList;
    private final ArrayList<String> onWayPassengerNameList;
    private final Activity activity;


    public RecyclerViewAdapterOnWay(Activity activity,
                                    ArrayList<String> onWayTaxiNameList,
                                    ArrayList<String> onWayPassengerNameList) {
        this.onWayTaxiNameList = onWayTaxiNameList;
        this.onWayPassengerNameList = onWayPassengerNameList;
        this.activity = activity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_on_way, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.taxiName.setText(onWayTaxiNameList.get(position));
        holder.passengerName.setText(onWayPassengerNameList.get(position));
    }

    @Override
    public int getItemCount() {
        return onWayTaxiNameList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView passengerName;
        private final TextView taxiName;

        MyViewHolder(View itemView) {
            super(itemView);
            passengerName = itemView.findViewById(R.id.card_view_passengerName);
            taxiName = itemView.findViewById(R.id.card_view_taxiName);
        }
    }
}