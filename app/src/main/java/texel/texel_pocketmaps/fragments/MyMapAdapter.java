package texel.texel_pocketmaps.fragments;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.model.MyMap;

/**
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on July 03, 2015.
 */
public class MyMapAdapter extends RecyclerView.Adapter<MyMapAdapter.ViewHolder> {
    private final List<MyMap> myMaps;
    private List<MyMap> myMapsFiltered;
    private final boolean isDownloadingView;

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyMapAdapter(List<MyMap> myMaps, boolean isDownloadingView) {
        this.myMaps = myMaps;
        this.myMapsFiltered = myMaps;
        this.isDownloadingView = isDownloadingView;
    }

    static void log(String txt) {
        Log.i(MyMapAdapter.class.getName(), txt);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_maps_item, parent, false);
        ViewHolder vh = new ViewHolder(v, isDownloadingView);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setItemData(myMapsFiltered.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return myMapsFiltered.size();
    }

    public void refreshMapView(MyMap myMap) {
        int rvIndex = myMapsFiltered.indexOf(myMap);
        if (rvIndex >= 0) {
            notifyItemRemoved(rvIndex);
            notifyItemInserted(rvIndex);
        } else {
            log("No map-entry for refreshing found, maybe filter active.");
        }
    }

    /**
     * @param position
     * @return MyMap item at the position
     */
    public MyMap getItem(int position) {
        return myMapsFiltered.get(position);
    }

    /**
     * remove item at the given position
     *
     * @param position index
     */
    public MyMap remove(int position) {
        MyMap mm = null;
        if (position >= 0 && position < getItemCount()) {
            mm = myMaps.remove(position);
            int posFiltered = myMapsFiltered.indexOf(mm);
            if (posFiltered >= 0) { // Filter is active!
                myMapsFiltered.remove(posFiltered);
                position = posFiltered;
            }
            notifyItemRemoved(position);
        }
        return mm;
    }

    /**
     * Clear the list (remove all elements)
     * Does NOT call notifyItemRangeRemoved()
     */
    public void clearList() {
        this.myMaps.clear();
        this.myMapsFiltered.clear();
    }

    /**
     * add a list of MyMap
     *
     * @param maps
     */
    public void addAll(List<MyMap> maps) {
        this.myMaps.addAll(maps);
        if (myMaps == myMapsFiltered) {
            notifyItemRangeInserted(myMaps.size() - maps.size(), maps.size());
        }
    }

    /**
     * @return a string list of map names (continent_country)
     */
    public List<String> getMapNameList() {
        ArrayList<String> al = new ArrayList<String>();
        for (MyMap mm : myMaps) {
            al.add(mm.getMapName());
        }
        return al;
    }

    public void doFilter(String filterText) {
        log("FILTER-START!");
        filterText = filterText.toLowerCase();
        List<MyMap> filteredList = new ArrayList<MyMap>();
        if (filterText.isEmpty()) {
            filteredList = myMaps;
            log("FILTER: Empty");
        } else {
            for (MyMap curMap : myMaps) {
                if (curMap.getCountry().toLowerCase().contains(filterText) || curMap.getContinent().toLowerCase().contains(filterText)) {
                    filteredList.add(curMap);
                }
            }
            log("FILTER: " + filteredList.size() + "/" + myMaps.size());
        }
        myMapsFiltered = filteredList;
        notifyDataSetChanged();
        log("FILTER: Publish: " + myMapsFiltered.size() + "/" + myMaps.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public FloatingActionButton flag;
        public TextView name, continent, size, downloadStatus;
        private final boolean isDownloadingView;

        protected ViewHolder(View itemView, boolean isDownloadingView) {
            super(itemView);
            this.isDownloadingView = isDownloadingView;
            this.flag = (FloatingActionButton) itemView.findViewById(R.id.my_maps_item_flag);
            this.name = (TextView) itemView.findViewById(R.id.my_maps_item_name);
            this.continent = (TextView) itemView.findViewById(R.id.my_maps_item_continent);
            this.size = (TextView) itemView.findViewById(R.id.my_maps_item_size);
            this.downloadStatus = (TextView) itemView.findViewById(R.id.my_maps_item_download_status);
        }

        public void setItemData(MyMap myMap) {
            name.setTextColor(android.graphics.Color.BLACK);
            if (isDownloadingView) {
                MyMap.DlStatus status = myMap.getStatus();

                if (status == MyMap.DlStatus.Downloading) {
                    flag.setImageResource(R.drawable.ic_pause_orange_24dp);
                    downloadStatus.setText("Yüklənir ...");
                }
            } else {
                downloadStatus.setText("yükləndi");
                flag.setImageResource(R.drawable.ic_map_white_24dp);
            }

            name.setText(myMap.getCountry());
            continent.setText(myMap.getContinent());
            size.setText(myMap.getSize());
        }
    }
}
