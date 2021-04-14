package texel.texel_pocketmaps.activities.SignActivities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.activities.RegisterActivities.AdminRegisterActivity;
import texel.texel_pocketmaps.activities.RegisterActivities.PassengerRegisterActivity;
import texel.texel_pocketmaps.activities.RegisterActivities.TaxiRegisterActivity;


public class SignUp extends AppCompatActivity implements View.OnClickListener {

    private TextView buttonNextToInformationPage;
    private EditText editTextEmail, editTextPasswordConfirm;
    private EditText editTextPassword;
    private int statusId = 3;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        activity = this;

        RadioGroup radioGroupUsers = findViewById(R.id.radioGroupUsers);
        radioGroupUsers.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.radioAdmin:
                    statusId = 1;
                    break;

                case R.id.radioOrderer:
                    statusId = 2;
                    break;

                case R.id.radioTaxi:
                    statusId = 3;
                    break;
            }
        });

        switch (statusId) {
            case 1:
                radioGroupUsers.check(R.id.radioAdmin);
                break;

            case 2:
                radioGroupUsers.check(R.id.radioOrderer);
                break;

            case 3:
                radioGroupUsers.check(R.id.radioTaxi);
                break;
        }

        buttonNextToInformationPage = findViewById(R.id.buttonNextToInformationPage);
        buttonNextToInformationPage.setOnClickListener(this);
        editTextPasswordConfirm = findViewById(R.id.editTextPasswordConfirm);
        editTextPasswordConfirm.setOnClickListener(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
    }


    @Override
    public void onClick(View view) {
        if (view == buttonNextToInformationPage) {
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();
            String passwordConfirm = editTextPasswordConfirm.getText().toString();

            if (email.length() < 5) {
                Toast.makeText(activity, R.string.enter_email, Toast.LENGTH_LONG).show();
                return;
            } else email += "@mail.ru";

            if (password.length() < 8) {
                Toast.makeText(activity, R.string.enter_password, Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(activity, R.string.password_not_matched, Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent;
            switch (statusId) {
                case 1:
                    intent = new Intent(activity, AdminRegisterActivity.class);
                    break;

                case 2:
                    intent = new Intent(activity, PassengerRegisterActivity.class);
                    break;

                case 3:
                    intent = new Intent(activity, TaxiRegisterActivity.class);
                    break;

                default:
                    return;
            }
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            startActivity(intent);
        }
    }
}
