package texel.texel_pocketmaps.activities.RegisterActivities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.InformationClasses.TaxiInformation;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;
import texel.texel_pocketmaps.activities.MainActivity;

public class TaxiRegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextSurname,
            editTextCarNumber, editTextCarColor, editTextCarYear, editTextCarModel,
            editTextPhoneNumber, editTextIdentificationCardNumber, editTextRegistrationNumber;
    private TextView textViewCategories;

    private CustomProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private ArrayList<DatabaseReference> databases = new ArrayList<>();

    private Activity activity;
    private String adminUsername, adminPassword;
    private int category_index = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi_register);
        activity = this;

        reLoadSeed();
    }

    private void reLoadSeed() {
        firebaseAuth = FirebaseAuth.getInstance();
        databases = DatabaseFunctions.getDatabases(this);

        TextView buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setOnClickListener(view -> registerUser());

        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
        editTextCarNumber = findViewById(R.id.editTextCarNumber);
        editTextCarColor = findViewById(R.id.editTextCarColor);
        editTextCarYear = findViewById(R.id.editTextCarYear);
        editTextCarModel = findViewById(R.id.editTextCarModel);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextIdentificationCardNumber = findViewById(R.id.editTextIdentificationCardNumber);
        editTextRegistrationNumber = findViewById(R.id.editTextRegistrationNumber);

        textViewCategories = findViewById(R.id.textViewCategories);
        textViewCategories.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(activity, R.style.MyCategoryDialogStyle);
            b.setTitle(getString(R.string.select_category));
            String[] types = {"Ekonom", "Komfort", "Kuryer"};
            b.setSingleChoiceItems(types, category_index, (dialog, which) -> {
                category_index = which;
                dialog.dismiss();
                textViewCategories.setText(types[category_index]);
            });
            b.show();
        });
    }

    private void registerUser() {
        adminUsername = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        final String email = getIntent().getStringExtra("email");
        final String password = getIntent().getStringExtra("password");
        final String name = editTextName.getText().toString();
        final String surname = editTextSurname.getText().toString();
        final String carNumber = editTextCarNumber.getText().toString();
        final String carColor = editTextCarColor.getText().toString();
        final String carYear = editTextCarYear.getText().toString();
        final String carModel = editTextCarModel.getText().toString();
        final String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        final String identificationCardNumber = editTextIdentificationCardNumber.getText().toString().trim();
        final String registrationNumber = editTextRegistrationNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(surname)) {
            Toast.makeText(this, R.string.enter_surname, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, R.string.enter_phone_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(identificationCardNumber)) {
            Toast.makeText(this, R.string.enter_identification_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(carNumber)) {
            Toast.makeText(this, R.string.enter_car_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(carColor)) {
            Toast.makeText(this, R.string.enter_car_color, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(carYear)) {
            Toast.makeText(this, R.string.enter_car_year, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(carModel)) {
            Toast.makeText(this, R.string.enter_car_model, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(registrationNumber)) {
            Toast.makeText(this, R.string.enter_register_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if (category_index == -1) {
            Toast.makeText(this, R.string.select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new CustomProgressDialog(this, getString(R.string.registering));
        progressDialog.setCancelable(false);
        progressDialog.show();

        databases.get(0).child("USERS/ADMIN/" + adminUsername.split("@")[0] + "/ABOUT/password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminPassword = snapshot.getValue(String.class);
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(TaxiRegisterActivity.this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        try {
                            TaxiInformation obj = new TaxiInformation(
                                    name + " " + surname,
                                    carNumber,
                                    carColor,
                                    carYear,
                                    carModel,
                                    phoneNumber,
                                    identificationCardNumber,
                                    registrationNumber,
                                    category_index,
                                    password);

                            databases.get(0).child("USERS/TAXI/" + email.split("@")[0] + "/ABOUT").setValue(obj);

                            finishAffinity();
                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            intent.putExtra("profile_name", "TAXI");
                            intent.putExtra("adminUsername", adminUsername);
                            intent.putExtra("adminPassword", adminPassword);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("AAAAA", e.toString());
                            progressDialog.dismiss();
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getBaseContext(), R.string.incorrect_email, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
            }
        });
    }
}
