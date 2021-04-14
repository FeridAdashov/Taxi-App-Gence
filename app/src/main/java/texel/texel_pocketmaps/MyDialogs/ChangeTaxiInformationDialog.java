package texel.texel_pocketmaps.MyDialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import texel.texel.gencetaxiapp.R;


public class ChangeTaxiInformationDialog extends DialogFragment {

    private final String[] taxiCategories = {"Ekonom", "Komfort", "Kuryer"};
    private EditText editTextName, editTextSurname, editTextPhoneNumber, editTextCarColor, editTextCarYear, editTextCarModel,
            editTextRegistrationNumber, editTextCarNumber, editTextIdentificationCardNumber, editTextBalance;
    private TextView textViewCategories;
    private AlertDialog dialog;
    private String name, surname, userName, password, phoneNumber, carNumber,
            carColor, carYear, carModel, registerNumber, idCardNumber;
    private double balance;
    private boolean b = false;
    private int category_index;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.change_taxi_information_dialog, null);

        TextView textViewUserName = view.findViewById(R.id.textViewUserName);
        TextView textViewPassword = view.findViewById(R.id.textViewPassword);
        editTextName = view.findViewById(R.id.editTextName);
        editTextSurname = view.findViewById(R.id.editTextSurname);
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber);
        editTextCarNumber = view.findViewById(R.id.editTextCarNumber);
        editTextCarColor = view.findViewById(R.id.editTextCarColor);
        editTextCarYear = view.findViewById(R.id.editTextCarYear);
        editTextCarModel = view.findViewById(R.id.editTextCarModel);
        editTextRegistrationNumber = view.findViewById(R.id.editTextRegistrationNumber);
        editTextIdentificationCardNumber = view.findViewById(R.id.editTextIdentificationCardNumber);
        editTextBalance = view.findViewById(R.id.editTextBalance);

        textViewCategories = view.findViewById(R.id.textViewCategories);
        textViewCategories.setOnClickListener(v -> {
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(getActivity(), R.style.MyCategoryDialogStyle);
            b.setTitle(getString(R.string.select_category));
            b.setSingleChoiceItems(taxiCategories, category_index, (dialog, which) -> {
                category_index = which;
                dialog.dismiss();
                textViewCategories.setText(taxiCategories[category_index]);
            });
            b.show();
        });

        builder.setPositiveButton(getString(R.string.save), null)
                .setNeutralButton(getString(R.string.delete_profile), null)
                .setNegativeButton(getString(R.string.m_cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                .setView(view);

        if (b) {
            textViewUserName.setText(userName);
            textViewPassword.setText(password);
            editTextName.setText(name);
            editTextSurname.setText(surname);
            editTextPhoneNumber.setText(phoneNumber);
            editTextCarNumber.setText(carNumber);
            editTextCarColor.setText(carColor);
            editTextCarYear.setText(carYear);
            editTextCarModel.setText(carModel);
            editTextRegistrationNumber.setText(registerNumber);
            editTextIdentificationCardNumber.setText(idCardNumber);
            editTextBalance.setText(String.valueOf(balance));

            if (category_index != -1)
                textViewCategories.setText(taxiCategories[category_index]);
        }

        dialog = builder.create();
        dialog.setOnShowListener(arg0 -> {
            Button positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positive.setTextColor(getResources().getColor(R.color.primary));
            positive.setOnClickListener(v -> {
                try {
                    if (TextUtils.isEmpty(editTextName.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextSurname.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_surname, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextPhoneNumber.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_phone_number, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextCarNumber.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_car_number, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (category_index == -1) {
                        Toast.makeText(getActivity(), R.string.select_category, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextRegistrationNumber.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_register_number, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextIdentificationCardNumber.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_identification_number, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(editTextBalance.getText().toString())) {
                        Toast.makeText(getContext(), R.string.enter_money, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveTaxiData(userName,
                            editTextName.getText().toString(),
                            editTextSurname.getText().toString(),
                            editTextPhoneNumber.getText().toString(),
                            editTextCarNumber.getText().toString(),
                            editTextCarColor.getText().toString(),
                            editTextCarYear.getText().toString(),
                            editTextCarModel.getText().toString(),
                            category_index,
                            editTextRegistrationNumber.getText().toString(),
                            editTextIdentificationCardNumber.getText().toString(),
                            Double.parseDouble(editTextBalance.getText().toString()));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Yanlış daxil etmə!!!", Toast.LENGTH_SHORT).show();
                }
            });
            Button neutral = dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
            neutral.setTextColor(getResources().getColor(android.R.color.black));
            neutral.setText(R.string.delete_profile);
            neutral.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(userName))
                    FirebaseDatabase.getInstance().getReference("USERS/TAXI/" + userName).removeValue();
            });

            Button negative = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            negative.setTextColor(getResources().getColor(R.color.primary));
            negative.setText(R.string.close);
            negative.setOnClickListener(v -> dialog.dismiss());
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    public void setDefaultValues(String userName, String password, String name, String phoneNumber,
                                 String carNumber, String carColor, String carYear, String carModel,
                                 Integer category_index, String registerNumber, String idCardNumber, Double balance) {
        this.userName = userName;
        this.password = password;
        this.name = name.split(" ")[0];
        this.surname = name.split(" ")[1];
        this.phoneNumber = phoneNumber;
        this.carNumber = carNumber;
        this.carColor = carColor;
        this.carYear = carYear;
        this.carModel = carModel;
        this.category_index = category_index == null ? -1 : category_index;
        this.idCardNumber = idCardNumber;
        this.registerNumber = registerNumber;
        this.balance = balance == null ? 0. : balance;
        b = true;
    }

    public void saveTaxiData(String userName, String name, String surname, String phoneNumber,
                             String carNumber, String carColor, String carYear, String carModel,
                             int category_index, String registerNumber, String idCardNumber, double balance) {
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("USERS/TAXI/" + userName + "/ABOUT");
            databaseReference.child("name").setValue(name + " " + surname);
            databaseReference.child("phoneNumber").setValue(phoneNumber);
            databaseReference.child("carNumber").setValue(carNumber);
            databaseReference.child("carColor").setValue(carColor);
            databaseReference.child("carYear").setValue(carYear);
            databaseReference.child("carModel").setValue(carModel);
            databaseReference.child("category").setValue(category_index);
            databaseReference.child("registrationNumber").setValue(registerNumber);
            databaseReference.child("identificationCardNumber").setValue(idCardNumber);
            databaseReference.child("balance").setValue(balance);

            dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
