package mx.uabc.ahrs;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.data.TSSBTSensor;
import mx.uabc.ahrs.helpers.Quaternion;

public class DeviceTestActivity extends AppCompatActivity {

    private int sensorDelay;
    private TSSBTSensor sensor;
    private TSSBTSensor carSensor;
    private SharedPreferencesManager sharedPreferencesManager;
    private boolean isCollecting;

    private float mPitch, mRoll;
    private double lastY, lastZ;

    @BindView(R.id.xTextView)
    TextView xTextView;
    @BindView(R.id.yTextView)
    TextView yTextView;
    @BindView(R.id.zTextView)
    TextView zTextView;
    @BindView(R.id.x2TextView)
    TextView x2TextView;
    @BindView(R.id.y2TextView)
    TextView y2TextView;
    @BindView(R.id.z2TextView)
    TextView z2TextView;
    @BindView(R.id.collectBtn)
    Button collectButton;

    @OnClick(R.id.collectBtn)
    public void recolectar() {

        isCollecting = !isCollecting;

        if (isCollecting) {
            handler.post(runnableCode);
        } else {
            handler.removeCallbacks(runnableCode);
        }
    }

    private Handler handler = new Handler();

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            float[] q1 = sensor.getFilteredTaredOrientationQuaternion();
            Quaternion headQuaternion = new Quaternion(q1[3], q1[0], q1[1], q1[2]);
            double[] headAngles = headQuaternion.toEulerAngles();

            String x = getString(R.string.orientation_angle, "X", headAngles[0]);
            String y = getString(R.string.orientation_angle, "Y", headAngles[1]);
            String z = getString(R.string.orientation_angle, "Z", headAngles[2]);

            xTextView.setText(x);
            yTextView.setText(y);
            zTextView.setText(z);

            float[] q2 = carSensor.getFilteredTaredOrientationQuaternion();
            Quaternion carQuaternion = new Quaternion(q2[3], q2[0], q2[1], q2[2]);
            double[] carAngles = carQuaternion.toEulerAngles();

            String x2 = getString(R.string.orientation_angle, "X", carAngles[0]);
            String y2 = getString(R.string.orientation_angle, "Y", carAngles[1]);
            String z2 = getString(R.string.orientation_angle, "Z", carAngles[2]);

            x2TextView.setText(x2);
            y2TextView.setText(y2);
            z2TextView.setText(z2);

            handler.postDelayed(this, sensorDelay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_test);
        ButterKnife.bind(this);

        sharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        sensorDelay = sharedPreferencesManager.getSamplingRate();
    }

    private void show(String text) {
        runOnUiThread(() -> Toast.makeText(DeviceTestActivity.this,
                text,
                Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();

        startBT();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopBT();
    }

    private void startBT() {

        try {

            sensor = new TSSBTSensor(sharedPreferencesManager
                    .getHeadSensorMacAddress());

            sensor.startStreaming();

            Toast.makeText(this, "Sensor conectado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "No fué posible conectarse al sensor",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void stopBT() {

        if (sensor != null) {
            if (sensor.isStreaming)
                sensor.stopStreaming();
            sensor.close();
        }

    }
}
