package mx.uabc.ahrs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import mx.uabc.ahrs.adapters.SettingsAdapter;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.dialogs.BluetoothDevicesDialogFragment;
import mx.uabc.ahrs.events.BluetoothSelectedEvent;
import mx.uabc.ahrs.models.Setting;

public class SettingsActivity extends AppCompatActivity {

    private static final int PERMISSION_ALL = 1;

    @BindView(R.id.listView)
    ListView listView;

    private SettingsAdapter settingsAdapter;
    private SharedPreferencesManager sharedPreferencesManager;

    @Subscribe
    public void onBluetoothSelectedEvent(BluetoothSelectedEvent event) {

        if (event.getReference().equals(BluetoothSelectedEvent.Reference.HEAD))
            sharedPreferencesManager.setHeadSensorMacAddress(event.getMacAddress());
        else if (event.getReference().equals(BluetoothSelectedEvent.Reference.CAR))
            sharedPreferencesManager.setCarSensorMacAddress(event.getMacAddress());

        fetchSettings();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(getResources().getString(R.string.configuraciones));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPreferencesManager = SharedPreferencesManager.getInstance(this);

        initUI();
        fetchSettings();
    }

    private void initUI() {

        settingsAdapter = new SettingsAdapter();
        listView.setAdapter(settingsAdapter);
        listView.setOnItemClickListener((adapterView, view, i, l) ->
        {
            switch (i) {
                case 0:
                    askForPermissions();
                    break;
                case 1:
                    showDevicesDialog(BluetoothDevicesDialogFragment.HEAD_REFERENCE);
                    break;
                case 2:
                    showDevicesDialog(BluetoothDevicesDialogFragment.CAR_REFERENCE);
                    break;
            }
        });
    }


    private void fetchSettings() {

        List<Setting> settingsList = new ArrayList<>();

        settingsList.add(new Setting("Otorgar permisos",
                "Locación, lectura y escritura"));
        settingsList.add(new Setting("Dirección MAC del sensor",
                sharedPreferencesManager.getHeadSensorMacAddress()));
        settingsList.add(new Setting("Dirección MAC de la cámara",
                sharedPreferencesManager.getCarSensorMacAddress()));

        settingsAdapter.addItems(settingsList);
    }

    private void askForPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String[] PERMISSIONS = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }

        }

    }

    private void showDevicesDialog(String reference) {
        FragmentManager fm = getSupportFragmentManager();
        BluetoothDevicesDialogFragment dialogFragment
                = BluetoothDevicesDialogFragment.newInstance(reference);
        dialogFragment.show(fm, "dialog_fragment");
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
