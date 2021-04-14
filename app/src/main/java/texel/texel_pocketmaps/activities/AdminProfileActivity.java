package texel.texel_pocketmaps.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import menu.library.FilterMenu;
import menu.library.FilterMenuLayout;
import texel.texel.gencetaxiapp.R;
import texel.texel_pocketmaps.HelperClasses.DatabaseFunctions;
import texel.texel_pocketmaps.HelperClasses.MaxMinValueFilter;
import texel.texel_pocketmaps.MyDialogs.DialogAskSomething;
import texel.texel_pocketmaps.activities.SignActivities.SignIn;
import texel.texel_pocketmaps.activities.SignActivities.SignUp;
import texel.texel_pocketmaps.admin_pages.Tariff.TariffActivity;
import texel.texel_pocketmaps.admin_pages.completed.CompletedFragment;
import texel.texel_pocketmaps.admin_pages.new_orders.NewOrdersFragment;
import texel.texel_pocketmaps.admin_pages.on_way.OnWayFragment;
import texel.texel_pocketmaps.admin_pages.searching_taxi.SearchingTaxiFragment;
import texel.texel_pocketmaps.admin_pages.taxis.TaxisFragment;
import texel.texel_pocketmaps.util.Variable;


public class AdminProfileActivity extends MapActivity {
    public FilterMenuLayout menuLayout1, menuLayout2;
    private ConstraintLayout menuConstraint1, menuConstraint2;
    private TextView textViewPageName;

    private AnimatorSet animSetXY;

    private String[] menu_1_names, menu_2_names;
    private int screenHeight, screenWidth, animMovingSizeX, animMovingSizeY;
    FilterMenu.OnMenuChangeListener listener1 = new FilterMenu.OnMenuChangeListener() {
        @Override
        public void onMenuItemClick(View view, int position) {
            if (animSetXY.isRunning()) return;

            boolean isChangingPageName = true;

            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new NewOrdersFragment();
                    break;

                case 1:
                    fragment = new SearchingTaxiFragment();
                    break;

                case 2:
                    fragment = new OnWayFragment();
                    break;

                case 3:
                    fragment = new CompletedFragment();
                    break;

                case 4:
                    startActivity(new Intent(getBaseContext(), MapActivityAdmin.class));
                    isChangingPageName = false;
                    break;
            }
            if (isChangingPageName) textViewPageName.setText(menu_1_names[position]);
            if (fragment != null) loadFragment(fragment);
        }

        @Override
        public void onMenuItemLongClick(View view, int position) {
            Toast toast = Toast.makeText(getApplicationContext(), menu_1_names[position], Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        @Override
        public void onMenuCollapse() {
            myAnimation(menuConstraint1, -animMovingSizeX, animMovingSizeY, 200);
        }

        @Override
        public void onMenuExpand() {
            if (menuLayout2.getState() == FilterMenuLayout.STATE_EXPAND)
                menuLayout2.collapse(true);

            myAnimation(menuConstraint1, (screenWidth - menuConstraint1.getWidth()) / 2.f,
                    -screenHeight / 2.f + menuConstraint1.getHeight(), 200);
        }
    };
    private Activity activity;
    FilterMenu.OnMenuChangeListener listener2 = new FilterMenu.OnMenuChangeListener() {
        @Override
        public void onMenuItemClick(View view, int position) {
            if (animSetXY.isRunning()) return;

            boolean isChangingPageName = true;

            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new TaxisFragment();
                    break;

                case 1:
                    isChangingPageName = false;
                    startActivity(new Intent(getBaseContext(), SignUp.class));
                    break;

                case 2:
                    isChangingPageName = false;
                    logOut();
                    break;

                case 3:
                    isChangingPageName = false;
                    openSettingChooseDialog();
                    break;
            }
            if (isChangingPageName) textViewPageName.setText(menu_2_names[position]);
            if (fragment != null) loadFragment(fragment);
        }

        @Override
        public void onMenuItemLongClick(View view, int position) {
            Toast toast = Toast.makeText(getApplicationContext(), menu_2_names[position], Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        @Override
        public void onMenuCollapse() {
            myAnimation(menuConstraint2, animMovingSizeX, animMovingSizeY, 200);
        }

        @Override
        public void onMenuExpand() {
            if (menuLayout1.getState() == FilterMenuLayout.STATE_EXPAND)
                menuLayout1.collapse(true);

            myAnimation(menuConstraint2, (-screenWidth + menuConstraint2.getWidth()) / 2.f,
                    -screenHeight / 2.f + menuConstraint2.getHeight(), 200);
        }
    };

    private void openSettingChooseDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.MyCategoryDialogStyle);
        b.setTitle(getString(R.string.select_category));
        String[] types = {"Tariflər", "Taksi faizi"};
        b.setSingleChoiceItems(types, 0, (dialog, which) -> {
            switch (which) {
                case 0:
                    startActivity(new Intent(activity, TariffActivity.class));
                    break;
                case 1:
                    changeTaxiPercentDialog();
                    break;
            }
            dialog.dismiss();
        });
        b.show();
    }

    private void changeTaxiPercentDialog() {
        final EditText input = new EditText(this);
        input.setTextColor(Color.parseColor("#FFFFFF"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setFilters(new InputFilter[]{new MaxMinValueFilter(0, 100)});

        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.MyCategoryDialogStyle);
        b.setTitle("Taksi Faizi");
        b.setMessage("Taksidən gediş haqqına uyğun olaraq tutulan faiz");
        b.setPositiveButton(R.string.save, (dialog, which) -> setTaxiPercentInDatabase(Double.parseDouble(input.getText().toString())));
        b.setNegativeButton(R.string.m_cancel, (dialog, which) -> dialog.dismiss());
        b.setView(input);
        b.show();
    }

    private void setTaxiPercentInDatabase(double percent) {
        DatabaseFunctions.getDatabases(activity).get(0).child("SETTING/taxiPercent").setValue(percent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        activity = this;

        Variable.getVariable().ensureLocationListenerActive = false;

        // size in dp in XML
        int FILTER_MENU_LAYOUT_WIDTH_AND_HEIGHT = 220;
        animMovingSizeX = (int) (FILTER_MENU_LAYOUT_WIDTH_AND_HEIGHT
                * getResources().getDisplayMetrics().density) / 2 - 100;
        animMovingSizeY = (int) (FILTER_MENU_LAYOUT_WIDTH_AND_HEIGHT
                * getResources().getDisplayMetrics().density) / 2 - 80;

        initViews();

        //loading the default fragment
        loadFragment(new NewOrdersFragment());
    }

    private void initViews() {
        textViewPageName = findViewById(R.id.textViewPageName);
        textViewPageName.setText(R.string.new_orders);

        View viewForClick = findViewById(R.id.viewForClick);
        viewForClick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (menuLayout1.getState() == FilterMenuLayout.STATE_EXPAND)
                        menuLayout1.collapse(true);
                    if (menuLayout2.getState() == FilterMenuLayout.STATE_EXPAND)
                        menuLayout2.collapse(true);
                }
                return false;
            }
        });

        menuLayout1 = findViewById(R.id.filter_menu1);
        attachMenu1(menuLayout1);

        menuLayout2 = findViewById(R.id.filter_menu2);
        attachMenu2(menuLayout2);

        menuConstraint1 = findViewById(R.id.menuConstraint1);
        myAnimation(menuConstraint1, -animMovingSizeX, animMovingSizeY, 0);

        menuConstraint2 = findViewById(R.id.menuConstraint2);
        myAnimation(menuConstraint2, animMovingSizeX, animMovingSizeY, 0);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    private void myAnimation(View view, float toX, float toY, long duration) {
        animSetXY = new AnimatorSet();
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", view.getTranslationX(), toX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), toY);

        animSetXY.playTogether(animX, animY);
        animSetXY.setDuration(duration);
        animSetXY.start();
    }

    private FilterMenu attachMenu1(FilterMenuLayout layout) {
        menu_1_names = new String[]{
                getString(R.string.new_orders),
                getString(R.string.searching_taxi),
                getString(R.string.orders_on_way),
                getString(R.string.completed_orders),
                getString(R.string.map)};

        return new FilterMenu.Builder(this)
                .addItem(R.drawable.ic_notifications_white)
                .addItem(R.drawable.ic_searching)
                .addItem(R.drawable.ic_taxi_nav_bar)
                .addItem(R.drawable.ic_flag_for_menu)
                .addItem(R.drawable.ic_map_white_24dp)
                .attach(layout)
                .withListener(listener1)
                .build();
    }

    private FilterMenu attachMenu2(FilterMenuLayout layout) {
        menu_2_names = new String[]{
                getString(R.string.taxis),
                getString(R.string.registration),
                getString(R.string.log_out),
                getString(R.string.settings)};

        return new FilterMenu.Builder(this)
                .addItem(R.drawable.ic_taxi_nav_bar)
                .addItem(R.drawable.ic_register_white_24)
                .addItem(R.drawable.ic_log_out)
                .addItem(R.drawable.ic_settings_white_24dp)
                .attach(layout)
                .withListener(listener2)
                .build();
    }

    private boolean loadFragment(Fragment fragment) {
        //switching fragment
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_admin, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void logOut() {
        View.OnClickListener positiveButtonListener = (view) -> {
            DialogAskSomething.alertDialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            finish();
            startActivity(new Intent(getBaseContext(), SignIn.class));
        };
        View.OnClickListener neutralButtonListener = (view) -> {
            DialogAskSomething.alertDialog.dismiss();
        };
        DialogAskSomething dialogAskSomething =
                new DialogAskSomething(getString(R.string.want_log_out),
                        getString(R.string.yes), "", getString(R.string.no),
                        positiveButtonListener, null, neutralButtonListener, true);
        dialogAskSomething.show(this);
    }
}