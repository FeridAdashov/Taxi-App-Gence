package texel.texel_pocketmaps.activities.HelperActivities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.DataClasses.Message;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;
import texel.texel_pocketmaps.Services.TaxiForegroundService;

public class PassengerTaxiChatActivity extends AppCompatActivity {

    private ArrayList<DatabaseReference> databases;

    private Activity activity;
    private String user_type;
    private boolean taxi_or_passenger;  // true = passenger, false = taxi

    private CustomProgressDialog progressDialog;
    private EditText editTextMessage;
    private ListView listViewChat;

    private ValueEventListener listener;
    private DatabaseReference listenerReference;
    private final ArrayList<Message> messageList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_taxi_chat);

        String order_name = getIntent().getStringExtra("order_name");
        if (TextUtils.isEmpty(order_name)) finish();

        taxi_or_passenger = TaxiForegroundService.isServiceActive;
        if (taxi_or_passenger) user_type = "taxi";
        else user_type = "passenger";

        databases = DatabaseFunctions.getDatabases(this);
        listenerReference = databases.get(1).child("ORDERS/ACTIVE/" + order_name + "/Chat");

        editTextMessage = findViewById(R.id.editTextMessage);

        ImageButton buttonSendMessage = findViewById(R.id.buttonSendMessage);
        buttonSendMessage.setOnClickListener(view -> {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            final String time = df.format(c.getTime());

            String s = editTextMessage.getText().toString().trim();
            if (!s.equals("")) {
                messageList.add(new Message(time, s, taxi_or_passenger));
                notifyAdapter();
                editTextMessage.setText("");

                listenerReference.child(user_type + "/" + time + "/message").setValue(s);
            }
        });

        listViewChat = findViewById(R.id.listViewChat);
        notifyAdapter();

        progressDialog = new CustomProgressDialog(this, getString(R.string.data_loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        listener = listenerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                try {
                    for (DataSnapshot snap : snapshot.child("taxi").getChildren()) {
                        String time = snap.getKey();

                        if (!taxi_or_passenger)
                            messageList.add(new Message(time, snap.child("message").getValue(String.class), false));
                        else
                            messageList.add(new Message(time, snap.child("message").getValue(String.class), true));
                    }

                    for (DataSnapshot snap : snapshot.child("passenger").getChildren()) {
                        String time = snap.getKey();

                        if (taxi_or_passenger)
                            messageList.add(new Message(time, snap.child("message").getValue(String.class), false));
                        else
                            messageList.add(new Message(time, snap.child("message").getValue(String.class), true));
                    }
                } catch (Exception e) {
                    stopProgress();
                    Log.d("AAAAA", e.toString());
                }
                stopProgress();

                Collections.sort(messageList, new CustomComparator());
                notifyAdapter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                stopProgress();
            }
        });
    }

    private void notifyAdapter() {
        ArrayAdapter<String> adapterMessages = new ArrayAdapter(this, R.layout.list_group_message, R.id.lblListHeader, getMessages()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView tw = row.findViewById(R.id.lblListHeader);
                TextView twTime = row.findViewById(R.id.lblListTime);

                if (!messageList.get(position).status) {
                    twTime.setText(messageList.get(position).time);
                    tw.setGravity(Gravity.LEFT);
                    tw.setTextColor(Color.parseColor("#FFFFFF"));
                    tw.setPadding(0, 0, 150, 10);
                } else if (messageList.get(position).status) {
                    twTime.setText(messageList.get(position).time);
                    tw.setGravity(Gravity.RIGHT);
                    tw.setTextColor(Color.parseColor("#F66C34"));
                    row.findViewById(R.id.lblListHeader).setPadding(150, 0, 0, 10);
                }
                return row;
            }
        };
        listViewChat.setAdapter(adapterMessages);
    }

    private void stopProgress() {
        progressDialog.dismiss();
    }

    private ArrayList<String> getMessages() {
        ArrayList<String> list = new ArrayList<>();
        for (Message message : messageList) {
            list.add(message.message);
            if (message.time.split(":").length > 2)
                message.time = message.time.substring(0, message.time.lastIndexOf(':'));
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        listenerReference.removeEventListener(listener);
    }

    private class CustomComparator implements Comparator<Message> {
        @Override
        public int compare(Message o1, Message o2) {
            String ob1 = o1.time;
            String ob2 = o2.time;

            return ob1.compareTo(ob2);
        }
    }
}
