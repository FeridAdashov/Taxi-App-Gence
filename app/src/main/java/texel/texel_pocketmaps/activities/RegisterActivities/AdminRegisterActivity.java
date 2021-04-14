package texel.texel_pocketmaps.activities.RegisterActivities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.InformationClasses.AdminInformation;
import texel.texel_pocketmaps.MyDialogs.CustomProgressDialog;
import texel.texel_pocketmaps.activities.AdminProfileActivity;

public class AdminRegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextSurname;

    private FirebaseAuth firebaseAuth;
    private ArrayList<DatabaseReference> databases = new ArrayList<>();

    private CustomProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register);
        reLoadSeed();
    }

    private void reLoadSeed() {
        firebaseAuth = FirebaseAuth.getInstance();
        databases = DatabaseFunctions.getDatabases(this);

        Button buttonRegisterStudent = findViewById(R.id.buttonRegister);
        buttonRegisterStudent.setOnClickListener(view -> registerUser());

        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
    }

    private void registerUser() {
        final String email = getIntent().getStringExtra("email");
        final String password = getIntent().getStringExtra("password");
        final String name = editTextName.getText().toString();
        final String surname = editTextSurname.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(surname)) {
            Toast.makeText(this, R.string.enter_surname, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new CustomProgressDialog(this, getString(R.string.registering));
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(AdminRegisterActivity.this, task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    AdminInformation obj = new AdminInformation(
                            name + " " + surname,
                            "",
                            password);

                    databases.get(0).child("USERS/ADMIN/" + email.split("@")[0] + "/ABOUT").setValue(obj);

                    finishAffinity();
                    Intent intent = new Intent(getBaseContext(), AdminProfileActivity.class);
                    startActivity(intent);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getBaseContext(), R.string.incorrect_email, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(Throwable::printStackTrace);
        } catch (Exception e) {
            Log.d("AAAAA", e.toString());
            progressDialog.dismiss();
        }
    }
}
