package texel.texel_pocketmaps.admin_pages.on_way;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class OnWayFragment extends Fragment {

    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private View view;

    private RecyclerViewAdapterOnWay adapter;
    private final ArrayList<String> taxiNameList = new ArrayList<>();
    private final ArrayList<String> passengerNameList = new ArrayList<>();
    private DatabaseReference databaseReferenceOrders;

    private Activity activity;
    private String today;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_on_way, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        activity = getActivity();
        textViewNoItemInfo = view.findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Yolda Olan Sifariş Yoxdur");

        databaseReferenceOrders = DatabaseFunctions.getDatabases(getContext()).get(1).child("ORDERS");
        today = new SimpleDateFormat("dd_MM_yyyy").format(new Date());

        progressDialog = new CustomProgressDialog(activity, getString(R.string.data_loading));

        configureListViewProducts();
    }

    private void configureListViewProducts() {
        adapter = new RecyclerViewAdapterOnWay(activity, taxiNameList, passengerNameList);
        RecyclerView myView = view.findViewById(R.id.recyclerviewOnWay);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadOnWayOrdersFromDatabase();
    }

    private void loadOnWayOrdersFromDatabase() {
        progressDialog.show();
        databaseReferenceOrders.child("ACTIVE").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                taxiNameList.clear();

                for (DataSnapshot snapshotOrder : dataSnapshot.getChildren()) {
                    boolean isOldOrder = false;
                    try {
                        isOldOrder = MyHelperFunctions.isBigDifference(snapshotOrder.getKey().split(" ")[0], 10 * 60);

                        if (isOldOrder) {
                            databaseReferenceOrders.child("DELETED/" + today + "/" + snapshotOrder.getKey())
                                    .setValue(snapshotOrder.getValue());
                            snapshotOrder.getRef().removeValue();
                        }
                    } catch (Exception e) {
                        Log.d("AAAAAA", e.toString());
                    }
                    final Integer type = snapshotOrder.child("type").getValue(Integer.class);
                    if (type == null || (type != 3 && type != 4) || isOldOrder) continue;

                    taxiNameList.add(snapshotOrder.child("taxiName").getValue(String.class));
                    passengerNameList.add(snapshotOrder.child("passengerName").getValue(String.class));
                }
                if (taxiNameList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
                else textViewNoItemInfo.setVisibility(View.VISIBLE);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }
}