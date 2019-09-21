package mx.uabc.ahrs;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import mx.uabc.ahrs.adapters.SettingsAdapter;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.models.Setting;

public class SettingsActivity extends AppCompatActivity {

    private static final int PERMISSION_ALL = 1;

    @BindView(R.id.listView)
    ListView listView;

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

        initUI();
    }

    private void initUI() {

        SharedPreferencesManager sharedPreferencesManager =
                SharedPreferencesManager.getInstance(this);

        SettingsAdapter settingsAdapter = new SettingsAdapter();
        settingsAdapter.addItem(new Setting("Otorgar permisos", "Locación"));
        settingsAdapter.addItem(new Setting("Dispositivo Bluetooth",
                sharedPreferencesManager.getBtDeviceName()));
        settingsAdapter.addItem(new Setting("Tasa de muestreo",
                sharedPreferencesManager.getSamplingRate() + " Hz"));

        listView.setAdapter(settingsAdapter);
        listView.setOnItemClickListener((adapterView, view, i, l) ->
        {
            if (i == 0) {
                askForPermissions();
            }
        });
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
}
