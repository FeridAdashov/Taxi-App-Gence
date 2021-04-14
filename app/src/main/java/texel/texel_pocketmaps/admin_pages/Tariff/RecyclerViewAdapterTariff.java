package texel.texel_pocketmaps.admin_pages.Tariff;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Tariff;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;

public class RecyclerViewAdapterTariff extends RecyclerView.Adapter<RecyclerViewAdapterTariff.MyViewHolder> {
    private final ArrayList<Tariff> tariffList;
    private final Activity activity;

    public RecyclerViewAdapterTariff(Activity activity, ArrayList<Tariff> tariffList) {
        this.tariffList = tariffList;
        this.activity = activity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_tariffs, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.name.setText((position + 1) + ". Tarif");
        holder.from.setText(String.valueOf(tariffList.get(position).getFrom()));
        holder.to.setText(String.valueOf(tariffList.get(position).getTo()));
        holder.every.setText(String.valueOf(tariffList.get(position).getEvery()));
        holder.money.setText(String.valueOf(tariffList.get(position).getMoney()));

        holder.delete.setOnClickListener(v -> deleteTariffs(position));
    }

    private void deleteTariffs(int position) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle("Tarifi sil");
        b.setMessage("Bu və sonraki tariflər silinəcək");
        b.setPositiveButton(R.string.delete, (dialog, which) -> {
            for (int i = getItemCount() - 1; i >= position; --i) {
                DatabaseFunctions.getDatabases(activity).get(0).child("SETTING/TARIFFS/" + i).removeValue();
                tariffList.remove(i);
            }
            notifyDataSetChanged();
        });
        b.setNegativeButton(R.string.m_cancel, (dialog, which) -> dialog.dismiss());
        b.show();
    }

    @Override
    public int getItemCount() {
        return tariffList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView from;
        private final TextView to;
        private final TextView every;
        private final TextView money;
        private final ImageButton delete;

        MyViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.card_view_name);
            from = itemView.findViewById(R.id.card_view_from);
            to = itemView.findViewById(R.id.card_view_to);
            every = itemView.findViewById(R.id.card_view_every);
            money = itemView.findViewById(R.id.card_view_money);
            delete = itemView.findViewById(R.id.deleteTarif);
        }
    }
}