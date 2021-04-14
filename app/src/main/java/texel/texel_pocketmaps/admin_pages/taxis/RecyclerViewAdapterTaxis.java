package texel.texel_pocketmaps.admin_pages.taxis;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.CardViewTaxiInfo;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.ChangeTaxiInformationDialog;

public class RecyclerViewAdapterTaxis extends RecyclerView.Adapter<RecyclerViewAdapterTaxis.MyViewHolder> {

    private final ArrayList<String> nameList;
    private final HashMap<String, CardViewTaxiInfo> hashMap;
    private final FragmentManager fragmentManager;
    private final Activity activity;

    public RecyclerViewAdapterTaxis(Activity activity,
                                    ArrayList<String> nameList,
                                    HashMap<String, CardViewTaxiInfo> hashMap,
                                    FragmentManager fragmentManager) {
        this.activity = activity;
        this.nameList = nameList;
        this.hashMap = hashMap;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_taxis, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        String name = nameList.get(position);
        holder.name.setText(name);

        if (hashMap.get(name).category == -1) {
            holder.category.setText(R.string.select_category);

            holder.category.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_taxi_no_category, 0, 0, 0);

            holder.viewLine.setBackgroundColor(activity.getResources().getColor(R.color.black));
        } else switch (hashMap.get(name).category) {
            case 0:
                holder.category.setText(R.string.econom);

                holder.category.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_taxi_econom, 0, 0, 0);

                holder.viewLine.setBackgroundColor(activity.getResources().getColor(R.color.econom_taxi_color));
                break;
            case 1:
                holder.category.setText(R.string.comfort);

                holder.category.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_taxi_comfort, 0, 0, 0);

                holder.viewLine.setBackgroundColor(activity.getResources().getColor(R.color.comfort_taxi_color));
                break;
            case 2:
                holder.category.setText(R.string.curier);

                holder.category.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_taxi_curier, 0, 0, 0);

                holder.viewLine.setBackgroundColor(activity.getResources().getColor(R.color.curier_taxi_color));
                break;
        }

        holder.buttonEdit.setOnClickListener(v -> DatabaseFunctions.getDatabases(activity).get(0)
                .child("USERS/TAXI/" + hashMap.get(name).userName + "/ABOUT").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ChangeTaxiInformationDialog dialog = new ChangeTaxiInformationDialog();
                        dialog.setDefaultValues(
                                hashMap.get(name).userName,
                                dataSnapshot.child("password").getValue(String.class),
                                dataSnapshot.child("name").getValue(String.class),
                                dataSnapshot.child("phoneNumber").getValue(String.class),
                                dataSnapshot.child("carNumber").getValue(String.class),
                                dataSnapshot.child("carColor").getValue(String.class),
                                dataSnapshot.child("carYear").getValue(String.class),
                                dataSnapshot.child("carModel").getValue(String.class),
                                dataSnapshot.child("category").getValue(Integer.class),
                                dataSnapshot.child("registrationNumber").getValue(String.class),
                                dataSnapshot.child("identificationCardNumber").getValue(String.class),
                                dataSnapshot.child("balance").getValue(Double.class));
                        dialog.show(fragmentManager, "");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }));
    }

    @Override
    public int getItemCount() {
        return nameList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView category;
        private final ImageButton buttonEdit;
        private final View viewLine;

        MyViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.cardTextViewName);
            category = itemView.findViewById(R.id.cardTextViewCategory);
            buttonEdit = itemView.findViewById(R.id.cardButtonEdit);
            viewLine = itemView.findViewById(R.id.cardViewLine);
        }
    }
}