package texel.texel_pocketmaps.admin_pages.Tariff;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Tariff;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;

public class TariffActivity extends AppCompatActivity {

    private final ArrayList<Tariff> tariffList = new ArrayList<>();
    private CustomProgressDialog progressDialog;
    private TextView textViewNoItemInfo;
    private RecyclerViewAdapterTariff adapter;

    private DatabaseReference databaseReferenceTariffs;

    private Activity activity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tariff);

        activity = this;
        textViewNoItemInfo = findViewById(R.id.textViewNoItemInfo);
        textViewNoItemInfo.setText("Tariff Yoxdur");

        FloatingActionButton addTariff = findViewById(R.id.addTariff);
        addTariff.setOnClickListener(v -> addNewTariff());

        databaseReferenceTariffs = DatabaseFunctions.getDatabases(activity).get(0).child("SETTING/TARIFFS");

        progressDialog = new CustomProgressDialog(activity, getString(R.string.data_loading));

        configureListViewProducts();
    }

    private void configureListViewProducts() {
        adapter = new RecyclerViewAdapterTariff(activity, tariffList);
        RecyclerView myView = findViewById(R.id.recyclerviewTariffs);
        myView.setHasFixedSize(true);
        myView.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(activity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        myView.setLayoutManager(llm);

        loadNewOrdersFromDatabase();
    }

    private void loadNewOrdersFromDatabase() {
        progressDialog.show();
        databaseReferenceTariffs.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                tariffList.clear();

                for (DataSnapshot snapshotTariff : dataSnapshot.getChildren())
                    tariffList.add(snapshotTariff.getValue(Tariff.class));

                if (tariffList.size() > 0) textViewNoItemInfo.setVisibility(View.GONE);
                else textViewNoItemInfo.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }

    private void addNewTariff() {
        final View view = getLayoutInflater().inflate(R.layout.new_tariff_dialog_view, null);
        final TextView name = view.findViewById(R.id.card_view_name);
        final TextView from = view.findViewById(R.id.card_view_from);
        final EditText to = view.findViewById(R.id.card_view_to);
        final EditText every = view.findViewById(R.id.card_view_every);
        final EditText money = view.findViewById(R.id.card_view_money);

        int list_size = tariffList.size();
        name.setText(list_size + 1 + ". Tarif");
        if (list_size > 0)
            from.setText(String.valueOf(tariffList.get(list_size - 1).getTo()));

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setPositiveButton(R.string.save, (dialog, which) -> {
        });
        b.setNegativeButton(R.string.m_cancel, (dialog, which) -> dialog.dismiss());
        b.setView(view);

        final AlertDialog dialog = b.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (TextUtils.isEmpty(from.getText()) || TextUtils.isEmpty(to.getText()) ||
                    TextUtils.isEmpty(every.getText()) || TextUtils.isEmpty(money.getText())) {
                Toast.makeText(activity, "Yanlış dəyər daxil edilib!", Toast.LENGTH_SHORT).show();
            } else {
                Double d_from = Double.parseDouble(from.getText().toString());
                Double d_to = Double.parseDouble(to.getText().toString());
                Double d_every = Double.parseDouble(every.getText().toString());
                Double d_money = Double.parseDouble(money.getText().toString());

                if (!(d_from == null || d_to == null || d_every == null || d_money == null ||
                        d_to - d_from < 0.0 || d_to - d_from < d_every || d_every < 100 || d_money < 0.0)) {
                    saveTariff(new Tariff(d_from, d_to, d_every, d_money));
                    dialog.dismiss();
                } else {
                    Toast.makeText(activity, "Yanlış dəyər daxil edilib!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void saveTariff(Tariff tariff) {
        tariffList.add(tariff);
        adapter.notifyDataSetChanged();

        databaseReferenceTariffs.child(String.valueOf(tariffList.size() - 1)).setValue(tariff);

        textViewNoItemInfo.setVisibility(View.GONE);
    }
}