package texel.texel_pocketmaps.activities.RegisterActivities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.InformationClasses.PassengerInformation;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;
import texel.texel_pocketmaps.activities.MainActivity;

public class PassengerRegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextSurname, editTextPhoneNumber;

    private FirebaseAuth firebaseAuth;
    private ArrayList<DatabaseReference> databases = new ArrayList<>();

    private CustomProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_register);

        reLoadSeed();
    }

    private void reLoadSeed() {
        firebaseAuth = FirebaseAuth.getInstance();
        databases = DatabaseFunctions.getDatabases(this);

        Button buttonRegisterStudent = findViewById(R.id.buttonRegister);
        buttonRegisterStudent.setOnClickListener(view -> registerUser());

        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
    }

    private void registerUser() {
        final String email = getIntent().getStringExtra("email");
        final String password = getIntent().getStringExtra("password");
        final String name = editTextName.getText().toString();
        final String surname = editTextSurname.getText().toString();
        final String phoneNumber = editTextPhoneNumber.getText().toString().trim();

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

        progressDialog = new CustomProgressDialog(this, getString(R.string.registering));
        progressDialog.setCancelable(false);
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(PassengerRegisterActivity.this, new OnCompleteListener<AuthResult>() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onComplete(@androidx.annotation.NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    try {
                        PassengerInformation obj = new PassengerInformation(
                                name + " " + surname,
                                phoneNumber,
                                password);

                        databases.get(0).child("USERS/PASSENGER/" + email.split("@")[0] + "/ABOUT").setValue(obj);

                        finishAffinity();
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        intent.putExtra("profile_name", "PASSENGER");
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.d("AAAAA", e.toString());
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getBaseContext(), R.string.incorrect_email, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
