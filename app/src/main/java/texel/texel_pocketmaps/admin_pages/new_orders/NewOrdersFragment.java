package texel.texel_pocketmaps.admin_pages.new_orders;

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

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Order;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class NewOrdersFragment extends Fragment {

    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private View view;

    private final ArrayList<Order> orderList = new ArrayList<>();
    private final ArrayList<String> orderNameList = new ArrayList<>();
    private RecyclerViewAdapterNewOrders adapter;

    private DatabaseReference databaseReferenceOrders;
    private ValueEventListener listener;

    private Activity activity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_orders, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        activity = getActivity();
        textViewNoItemInfo = view.findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Yeni Sifariş Yoxdur");

        databaseReferenceOrders = DatabaseFunctions.getDatabases(getContext()).get(1).child("ORDERS/ACTIVE");

        progressDialog = new CustomProgressDialog(activity, getString(R.string.data_loading));

        configureListViewProducts();
    }

    private void configureListViewProducts() {
        adapter = new RecyclerViewAdapterNewOrders(activity, orderNameList, orderList);
        RecyclerView myView = view.findViewById(R.id.recyclerviewOrders);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadNewOrdersFromDatabase();
    }

    private void loadNewOrdersFromDatabase() {
        progressDialog.show();
        listener = databaseReferenceOrders.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                orderNameList.clear();
                orderList.clear();

                for (DataSnapshot snapshotOrder : dataSnapshot.getChildren()) {
                    final Integer type = snapshotOrder.child("type").getValue(Integer.class);
                    if (type == null || type != 1) continue;

                    Order order = snapshotOrder.getValue(Order.class);
                    orderList.add(order);
                    orderNameList.add(snapshotOrder.getKey());
                }
                if (orderList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
                else textViewNoItemInfo.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        orderList.clear();
        orderNameList.clear();
        adapter.notifyDataSetChanged();
        databaseReferenceOrders.removeEventListener(listener);
    }
}