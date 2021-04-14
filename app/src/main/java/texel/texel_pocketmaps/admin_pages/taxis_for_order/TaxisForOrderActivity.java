package texel.texel_pocketmaps.admin_pages.taxis_for_order;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.oscim.core.GeoPoint;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MyHelperFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class TaxisForOrderActivity extends AppCompatActivity {

    private final ArrayList<String> taxiNameList = new ArrayList<>();
    private final ArrayList<String> taxiUserNameList = new ArrayList<>();
    private final ArrayList<GeoPoint> taxiLocationList = new ArrayList<>();
    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private RecyclerViewAdapterTaxisForOrder adapter;
    private DatabaseReference databaseReferenceTaxis;

    private String orderName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxis_for_order);

        textViewNoItemInfo = findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Aktiv Taksi Yoxdur");

        orderName = getIntent().getStringExtra("orderName");
        databaseReferenceTaxis = DatabaseFunctions.getDatabases(this).get(0).child("USERS/TAXI");

        progressDialog = new CustomProgressDialog(this, getString(R.string.data_loading));

        configureListViewProducts();
    }

    private void configureListViewProducts() {
        adapter = new RecyclerViewAdapterTaxisForOrder(this,
                taxiUserNameList, taxiNameList, taxiLocationList, orderName);
        RecyclerView myView = findViewById(R.id.recyclerviewTaxisForOrder);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadDataFromDatabase();
    }

    private void loadDataFromDatabase() {
        progressDialog.show();
        databaseReferenceTaxis.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot taxiSnapShot : snapshot.getChildren()) {
                    Boolean active = taxiSnapShot.child("active").getValue(Boolean.class);
                    if (active == null || !active) continue;
                    if (MyHelperFunctions.isBigDifference(taxiSnapShot.child("lastActiveTime").getValue(String.class), 60))
                        continue;

                    Double lat = taxiSnapShot.child("LOCATION/lat").getValue(Double.class);
                    if (lat == null || lat == 0.) continue;

                    Double lon = taxiSnapShot.child("LOCATION/lon").getValue(Double.class);
                    if (lon == null || lon == 0.) continue;

                    if (!TextUtils.isEmpty(taxiSnapShot.child("orderName").getValue(String.class)))
                        continue;

                    taxiNameList.add(taxiSnapShot.child("ABOUT/name").getValue(String.class));
                    taxiUserNameList.add(taxiSnapShot.getKey());
                    taxiLocationList.add(new GeoPoint(lat, lon));
                }
                if (taxiNameList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
                else textViewNoItemInfo.setVisibility(View.VISIBLE);

                adapter.notifyDataSetChanged();
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("AAAAAAA", error.toString());
                progressDialog.dismiss();
            }
        });
    }
}
