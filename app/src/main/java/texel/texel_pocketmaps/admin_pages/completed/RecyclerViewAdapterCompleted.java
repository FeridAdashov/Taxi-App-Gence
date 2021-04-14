package texel.texel_pocketmaps.admin_pages.completed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;

public class RecyclerViewAdapterCompleted extends RecyclerView.Adapter<RecyclerViewAdapterCompleted.MyViewHolder> {

    private final ArrayList<String> nameList;
    private final ArrayList<String> timeList;

    public RecyclerViewAdapterCompleted(ArrayList<String> nameList, ArrayList<String> timeList) {
        this.nameList = nameList;
        this.timeList = timeList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_completed, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.name.setText(nameList.get(position));
        holder.time.setText(timeList.get(position));
    }

    @Override
    public int getItemCount() {
        return nameList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView time;

        MyViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.cardTextViewPassenger);
            time = itemView.findViewById(R.id.cardTextVieTime);
        }
    }
}