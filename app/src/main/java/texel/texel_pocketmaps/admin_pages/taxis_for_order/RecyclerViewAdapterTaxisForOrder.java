package texel.texel_pocketmaps.admin_pages.taxis_for_order;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.Date;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.map.MapHandler;

public class RecyclerViewAdapterTaxisForOrder extends RecyclerView.Adapter<RecyclerViewAdapterTaxisForOrder.MyViewHolder> {
    private final Activity activity;
    private final ArrayList<String> taxiUserNameList;
    private final ArrayList<String> taxiNameList;
    private final ArrayList<GeoPoint> taxiLocationList;
    private final String orderName;
    private final DatabaseReference databaseReference;
    private GeoPoint orderLocation;

    public RecyclerViewAdapterTaxisForOrder(Activity activity,
                                            ArrayList<String> taxiUserNameList,
                                            ArrayList<String> taxiNameList,
                                            ArrayList<GeoPoint> taxiLocationList,
                                            String orderName) {
        this.activity = activity;
        this.taxiUserNameList = taxiUserNameList;
        this.taxiNameList = taxiNameList;
        this.taxiLocationList = taxiLocationList;
        this.orderName = orderName;

        databaseReference = DatabaseFunctions.getDatabases(activity).get(1).child("ORDERS/ACTIVE/" + orderName);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("p1Lat").getValue(Double.class);
                Double lon = snapshot.child("p1Lon").getValue(Double.class);
                if (lat != null && lon != null)
                    orderLocation = new GeoPoint(lat, lon);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("AAAAAAAA", error.toString());
            }
        });
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_taxis_for_order, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.name.setText(taxiNameList.get(position));

        try {
            MapHandler.getMapHandler().calcPath(
                    taxiLocationList.get(position).getLatitude(),
                    taxiLocationList.get(position).getLongitude(),
                    orderLocation.getLatitude(),
                    orderLocation.getLongitude(),
                    activity,
                    holder.distance, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        holder.buttonDirect.setOnClickListener(v -> openDialogForDirect(position));
    }

    @Override
    public int getItemCount() {
        return taxiNameList.size();
    }

    private void openDialogForDirect(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Yönəldilsin?");
        builder.setNeutralButton(R.string.no, null);
        builder.setNegativeButton(R.string.yes, (dialog, which) -> {
            databaseReference.child("type").setValue(2);
            databaseReference.child("taxiName").setValue(taxiNameList.get(position));
            databaseReference.child("taxiUserName").setValue(taxiUserNameList.get(position));
            databaseReference.child("taxiTimer").setValue(MyHelperFunctions.dateFormatter.format(new Date()));
            DatabaseFunctions.getDatabases(activity).get(0)
                    .child("USERS/TAXI/" + taxiUserNameList.get(position) + "/orderName").setValue(orderName);
            activity.finish();
        });
        builder.create().show();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView distance;
        private final Button buttonDirect;

        MyViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.card_view_name);
            distance = itemView.findViewById(R.id.card_view_distance);
            buttonDirect = itemView.findViewById(R.id.cardButtonDirect);
        }
    }
}