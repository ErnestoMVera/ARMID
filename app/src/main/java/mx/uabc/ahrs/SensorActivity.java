package mx.uabc.ahrs;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.data.SensorManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.fragments.ClassifierFragment;
import mx.uabc.ahrs.fragments.RecollectionFragment;
import mx.uabc.ahrs.fragments.TrainingFragment;
import mx.uabc.ahrs.helpers.Quaternion;

public class SensorActivity extends AppCompatActivity {

    public static final String USER_ID_PARAM = "userIdParam";
    private int userId;
    private boolean isCalibrated;
    SensorManager sensorManager;
    Handler handler = new Handler();

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            float[] q = sensorManager.getFilteredTaredOrientationQuaternion();
            Quaternion quaternion = new Quaternion(q[3], q[0], q[1], q[2]);
            double[] angles = quaternion.toEulerAngles();
            long timestamp = System.currentTimeMillis();

            EventBus.getDefault().post(new SensorReadingEvent(angles[0], angles[1], angles[2], timestamp));

            handler.postDelayed(this, 200);
        }
    };

    @Subscribe
    public void onSensorStreamingEvent(SensorStreamingEvent sensorStreamingEvent) {

        if (!isCalibrated) {
            showCalibrationDialog();
            return;
        }

        if (sensorStreamingEvent.getAction() == SensorStreamingEvent.START) {
            handler.post(runnableCode);
        } else if (sensorStreamingEvent.getAction() == SensorStreamingEvent.STOP) {
            handler.removeCallbacks(runnableCode);
            Toast.makeText(this, "Sensado detenido", Toast.LENGTH_SHORT).show();
        }

    }

    private void showCalibrationDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Calibración del sensor")
                .setMessage("Antes de utilizar el sensor es necesario calibrarlo")
                .setPositiveButton(android.R.string.yes, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sensor_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (sensorManager == null)
            return false;

        if (item.getItemId() == R.id.tare_sensor) {

            sensorManager.setTareCurrentOrient();
            isCalibrated = true;
            Toast.makeText(this, "Sensor calibrado", Toast.LENGTH_SHORT).show();

            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        assert getIntent() != null;
        userId = getIntent().getIntExtra(USER_ID_PARAM, -1);
        User user = DatabaseManager.getInstance(this).getUserDao().loadById(userId);

        if (userId == -1 || user == null) {
            Toast.makeText(this, "Hubo un problema", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(user.name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.navigation_training:
                    changeFragment(TrainingFragment.newInstance(userId));
                    return true;
                case R.id.navigation_classificator:
                    changeFragment(ClassifierFragment.newInstance(userId));
                    return true;
                case R.id.navigation_recollection:
                    changeFragment(RecollectionFragment.newInstance(userId));
                    return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.navigation_training);
    }

    private void startBT() {

        try {
            sensorManager = SensorManager.getInstance();
            sensorManager.startStreaming();
            Toast.makeText(this, "Sensor conectado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "No fué posible conectarse al sensor",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void stopBT() {

        if (sensorManager.isStreaming)
            sensorManager.stopStreaming();

    }

    private void changeFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container,
                        fragment,
                        fragment.getClass().getCanonicalName())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        startBT();
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        stopBT();
        super.onStop();
    }
}
