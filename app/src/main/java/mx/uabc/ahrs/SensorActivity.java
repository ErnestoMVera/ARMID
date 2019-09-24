package mx.uabc.ahrs;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Date;

import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.data.TSSBTSensor;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.fragments.ClassifierFragment;
import mx.uabc.ahrs.fragments.RecollectionFragment;
import mx.uabc.ahrs.fragments.TrainingFragment;
import mx.uabc.ahrs.fragments.ValidationFragment;
import mx.uabc.ahrs.helpers.Quaternion;
import mx.uabc.ahrs.helpers.Utils;

public class SensorActivity extends AppCompatActivity {

    public static final String USER_ID_PARAM = "userIdParam";

    private int userId;
    private long sensorDelay;

    private SharedPreferencesManager sharedPreferencesManager;
    private TSSBTSensor headSensor;
    private Handler handler = new Handler();
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            float[] q1 = headSensor.getFilteredTaredOrientationQuaternion();
            Quaternion headQuaternion = new Quaternion(q1[3], q1[0], q1[1], q1[2]);
            double[] headAngles = headQuaternion.toEulerAngles();

            double pitch = headAngles[0];
            double roll = headAngles[1];
            double yaw = headAngles[2];

            long timestamp = System.currentTimeMillis();

            EventBus.getDefault().post(new SensorReadingEvent(pitch, roll, yaw, timestamp));

            handler.postDelayed(this, sensorDelay);
        }
    };

    @Subscribe
    public void onSensorStreamingEvent(SensorStreamingEvent sensorStreamingEvent) {

        if (sensorStreamingEvent.getAction() == SensorStreamingEvent.START) {
            handler.post(runnableCode);
        } else if (sensorStreamingEvent.getAction() == SensorStreamingEvent.STOP) {
            handler.removeCallbacks(runnableCode);
            Toast.makeText(this, "Censado detenido", Toast.LENGTH_SHORT).show();
        }

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

        sharedPreferencesManager
                = SharedPreferencesManager.getInstance(this);

        sensorDelay = Utils
                .hertzToMilliseconds(sharedPreferencesManager.getSamplingRate());

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
                case R.id.navigation_validator:
                    changeFragment(ValidationFragment.newInstance(userId));
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

            headSensor = new TSSBTSensor(sharedPreferencesManager.getHeadSensorMacAddress());
            headSensor.startStreaming();

            Toast.makeText(this, "Sensor de cabeza conectado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "No fué posible conectarse al sensor",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void stopBT() {

        if (headSensor != null) {
            if (headSensor.isStreaming)
                headSensor.stopStreaming();
            headSensor.close();
        }
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
