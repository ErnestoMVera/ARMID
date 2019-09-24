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

    @BindView(R.id.xTextView)
    TextView xTextView;
    @BindView(R.id.yTextView)
    TextView yTextView;
    @BindView(R.id.zTextView)
    TextView zTextView;
    @BindView(R.id.collectBtn)
    Button collectButton;

    @OnClick(R.id.collectBtn)
    public void recolectar() {

        isCollecting = !isCollecting;

        if (isCollecting)
            handler.post(runnableCode);
        else
            handler.removeCallbacks(runnableCode);
    }

    private Handler handler = new Handler();

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            float[] q1 = sensor.getFilteredTaredOrientationQuaternion();
            Quaternion headQuaternion = new Quaternion(q1[3], q1[0], q1[1], q1[2]);
            double[] headAngles = headQuaternion.toEulerAngles();

            float[] q2 = carSensor.getFilteredTaredOrientationQuaternion();
            Quaternion carQuaternion = new Quaternion(q2[3], q2[0], q2[1], q2[2]);
            double[] carAngles = carQuaternion.toEulerAngles();

            double pitch = headAngles[0];
            double roll = headAngles[1];

            double headYaw = headAngles[2];
            double carYaw = carAngles[2];

            double yaw = Math.abs(headYaw) - Math.abs(carYaw);

            String x = getString(R.string.orientation_angle, "X", pitch);
            String y = getString(R.string.orientation_angle, "Y", roll);
            String z = getString(R.string.orientation_angle, "Z", yaw);

            xTextView.setText(x);
            yTextView.setText(y);
            zTextView.setText(z);

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

    @Override
    protected void onStart() {
        startBT();
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopBT();
        super.onStop();
    }

    private void startBT() {

        try {

            sensor = new TSSBTSensor(sharedPreferencesManager
                    .getHeadSensorMacAddress());

            sensor.startStreaming();

            carSensor = new TSSBTSensor(sharedPreferencesManager
                    .getCarSensorMacAddress());

            carSensor.startStreaming();

            Toast.makeText(this, "Sensores conectados", Toast.LENGTH_SHORT).show();

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

        if (carSensor != null) {
            if (carSensor.isStreaming)
                carSensor.stopStreaming();
            carSensor.close();
        }
    }

}
