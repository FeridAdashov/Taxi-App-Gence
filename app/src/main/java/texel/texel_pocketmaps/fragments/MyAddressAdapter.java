package texel.texel_pocketmaps.fragments;

import android.location.Address;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.geocoding.AddressLoc;
import texel.texel_pocketmaps.model.listeners.OnClickAddressListener;

/**
 * This file is part of PocketMaps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on July 03, 2015.
 */
public class MyAddressAdapter extends RecyclerView.Adapter<MyAddressAdapter.ViewHolder> {
    private final List<Address> addressList;
    private final OnClickAddressListener onClickAddressListener;
    private final OnClickAddressListener onClickDetailsListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAddressAdapter(List<Address> addressList, OnClickAddressListener onClickAddressListener, OnClickAddressListener onClickDetailsListener) {
        this.addressList = addressList;
        this.onClickAddressListener = onClickAddressListener;
        this.onClickDetailsListener = onClickDetailsListener;
    }

    public static void log(String s) {
        Log.i(MyAddressAdapter.class.getName(), s);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_entry, parent, false);
        return new ViewHolder(v, onClickAddressListener, onClickDetailsListener);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.setItemData(addressList.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return addressList.size();
    }

    /**
     * @param position
     * @return MyMap item at the position
     */
    public Address getItem(int position) {
        return addressList.get(position);
    }

    /**
     * remove item at the given position
     *
     * @param position
     * @return The removed item.
     */
    public Address remove(int position) {
        Address mm = null;
        if (position >= 0 && position < getItemCount()) {
            mm = addressList.remove(position);
            notifyItemRemoved(position);
        }
        return mm;
    }

    /**
     * remove all items
     */
    public void removeAll() {
        int i = addressList.size();
        addressList.clear();
        notifyItemRangeRemoved(0, i);
    }

    public void addAll(List<Address> addr) {
        this.addressList.addAll(addr);
        notifyItemRangeInserted(addressList.size() - addr.size(), addr.size());
    }

    public void insert(Address address) {
        addressList.add(address);
        notifyItemInserted(getItemCount() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public OnClickAddressListener onClickAddressListener;
        public OnClickAddressListener onClickDetailsListener;
        public TextView firstLine, secondLine;
        ImageView addrDetailsButton;

        public ViewHolder(View itemView, OnClickAddressListener onClickAddressListener, OnClickAddressListener onClickDetailsListener) {
            super(itemView);
            this.onClickAddressListener = onClickAddressListener;
            this.onClickDetailsListener = onClickDetailsListener;
            this.firstLine = (TextView) itemView.findViewById(R.id.mapFirstLineTxt);
            this.secondLine = (TextView) itemView.findViewById(R.id.mapSecondLineTxt);
            this.addrDetailsButton = (ImageView) itemView.findViewById(R.id.iconAddressDetail);
        }

        public void setItemData(final Address address) {
            View.OnClickListener clickListener = v -> {
                log("onClick: " + itemView.toString());
                onClickAddressListener.onClick(address);
            };
            View.OnClickListener clickDetListener = v -> {
                log("onClick: " + itemView.toString());
                onClickDetailsListener.onClick(address);
            };
            firstLine.setOnClickListener(clickListener);
            secondLine.setOnClickListener(clickListener);
            addrDetailsButton.setOnClickListener(clickDetListener);

            ArrayList<String> lines = AddressLoc.getLines(address);
            while (lines.size() < 2) lines.add("");

            setText(firstLine, lines.get(0));
            setText(secondLine, strJoin(lines, 1, ", "));
        }

        private void setText(TextView curLine, String addressLine) {
            if (addressLine == null || addressLine.isEmpty()) curLine.setText("");
            else curLine.setText(addressLine);
        }

        public String strJoin(ArrayList<String> arrayList, int beginIndex, String joinCharacter) {
            StringBuilder sbStr = new StringBuilder();
            for (int i = beginIndex; i < arrayList.size(); i++) {
                if (i > beginIndex) sbStr.append(joinCharacter);
                sbStr.append(arrayList.get(i));
            }
            return sbStr.toString();
        }
    }
}
