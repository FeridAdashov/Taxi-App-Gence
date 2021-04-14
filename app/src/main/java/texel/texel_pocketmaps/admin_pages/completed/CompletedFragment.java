package texel.texel_pocketmaps.admin_pages.completed;

import android.app.Activity;
import android.os.Bundle;
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

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class CompletedFragment extends Fragment {

    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private View view;

    private final ArrayList<String> passengerList = new ArrayList<>();
    private final ArrayList<String> timeList = new ArrayList<>();
    private RecyclerViewAdapterCompleted adapter;
    private DatabaseReference databaseReferenceOrders;

    private Activity activity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_completed, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        activity = getActivity();
        textViewNoItemInfo = view.findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Tamamlanmış Sifariş Yoxdur");

        String today = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
        databaseReferenceOrders = DatabaseFunctions.getDatabases(getContext()).get(1).child("ORDERS/COMPLETED/" + today);

        progressDialog = new CustomProgressDialog(activity, getString(R.string.data_loading));

        configureListViewProducts();
    }

    private void configureListViewProducts() {
        adapter = new RecyclerViewAdapterCompleted(passengerList, timeList);
        RecyclerView myView = view.findViewById(R.id.recyclerviewCompletedOrders);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadCompletedOrdersFromDatabase();
    }

    private void loadCompletedOrdersFromDatabase() {
        progressDialog.show();
        databaseReferenceOrders.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                passengerList.clear();
                timeList.clear();

                for (DataSnapshot snapshotOrder : dataSnapshot.getChildren()) {
                    passengerList.add(snapshotOrder.child("passengerName").getValue(String.class));
                    timeList.add(snapshotOrder.getKey().substring(11, 19));
                }
                if (passengerList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
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