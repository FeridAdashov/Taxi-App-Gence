package texel.texel_pocketmaps.admin_pages.taxis;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.util.HashMap;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.CardViewTaxiInfo;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class TaxisFragment extends Fragment {

    private final HashMap<String, CardViewTaxiInfo> cardViewTaxiInfoHashMap = new HashMap<>();
    private final ArrayList<String> nameList = new ArrayList<>();
    private final ArrayList<String> searchNameList = new ArrayList<>();
    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private View view;
    private DatabaseReference databaseReferenceTaxis;
    private RecyclerViewAdapterTaxis adapter;

    private Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_taxis, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        activity = getActivity();
        textViewNoItemInfo = view.findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Qeydiyyatda Olan Taksi Yoxdur");

        progressDialog = new CustomProgressDialog(activity, getString(R.string.data_loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        databaseReferenceTaxis = DatabaseFunctions.getDatabases(getContext()).get(0).child("USERS/TAXI");

        configureListViewTaxis();
        configureSearchEditText();
    }

    private void configureSearchEditText() {
        EditText etSearch = view.findViewById(R.id.editTextSearch);
        etSearch.clearFocus();
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchNameList.clear();

                String search = s.toString().toLowerCase();

                for (String name : nameList)
                    if (name.toLowerCase().contains(search)) searchNameList.add(name);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void configureListViewTaxis() {
        adapter = new RecyclerViewAdapterTaxis(activity, searchNameList, cardViewTaxiInfoHashMap, getChildFragmentManager());
        RecyclerView myView = view.findViewById(R.id.recyclerviewTaxis);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadNewOrdersFromDatabase();
    }

    private void loadNewOrdersFromDatabase() {
        databaseReferenceTaxis.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                cardViewTaxiInfoHashMap.clear();
                nameList.clear();
                searchNameList.clear();

                for (DataSnapshot snapshotTaxis : dataSnapshot.getChildren()) {
                    Integer type = snapshotTaxis.child("ABOUT/statusId").getValue(Integer.class);
                    if (type == null || type != 3) continue;

                    String name = snapshotTaxis.child("ABOUT/name").getValue(String.class);
                    nameList.add(name);

                    cardViewTaxiInfoHashMap.put(name,
                            new CardViewTaxiInfo(snapshotTaxis.getKey(),
                                    snapshotTaxis.child("ABOUT/category").getValue(Integer.class),
                                    snapshotTaxis.child("active").getValue(Boolean.class)));
                }
                if (nameList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
                else textViewNoItemInfo.setVisibility(View.VISIBLE);

                progressDialog.dismiss();
                searchNameList.addAll(nameList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }
}