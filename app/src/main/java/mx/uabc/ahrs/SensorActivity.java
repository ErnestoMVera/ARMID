package mx.uabc.ahrs;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.ControllerEvent;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.fragments.ClassifierFragment;
import mx.uabc.ahrs.fragments.RecollectionFragment;
import mx.uabc.ahrs.fragments.TrainingFragment;
import mx.uabc.ahrs.fragments.ValidationFragment;
import mx.uabc.ahrs.services.BluetoothSensorService;
import mx.uabc.ahrs.services.BluetoothService;

import static android.view.KeyEvent.KEYCODE_BUTTON_B;
import static android.view.KeyEvent.KEYCODE_BUTTON_L1;
import static android.view.KeyEvent.KEYCODE_BUTTON_R1;
import static android.view.KeyEvent.KEYCODE_BUTTON_Y;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

public class SensorActivity extends AppCompatActivity {

    // Debugging
    private static final String TAG = "SensorActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int GYROSCOPE_INDEX = 0;
    public static final int ORIENTATION_INDEX = 1;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String USER_ID_PARAM = "userIdParam";

    private int lecturaActual = 0;
    private int userId;
    private double lastY, lastZ;
    private double magGyro;

    private SharedPreferencesManager sharedPreferencesManager;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothService mBluetoothService = null;
    private BluetoothSensorService mBluetoothSensorService = null;

    ArrayList<Integer> buttonActions =
            new ArrayList<>(Arrays.asList(KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT,
                    KEYCODE_DPAD_UP, KEYCODE_BUTTON_B, KEYCODE_BUTTON_Y,
                    KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1));

    @Subscribe
    public void onSensorStreamingEvent(SensorStreamingEvent sensorStreamingEvent) {

        if (sensorStreamingEvent.getAction() == SensorStreamingEvent.START) {
            startSensorStreaming();
        } else if (sensorStreamingEvent.getAction() == SensorStreamingEvent.STOP) {
            stopSensorStreaming();
        }

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (buttonActions.contains(event.getKeyCode())
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            dispatchButtonAction(event.getKeyCode());
            return true;
        }

        return false;
    }

    private void dispatchButtonAction(int keyCode) {
        EventBus.getDefault()
                .post(new ControllerEvent(keyCode));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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


        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(user.name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.navigation_training:
                    changeFragment(TrainingFragment.newInstance(userId));
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

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container,
                        fragment,
                        fragment.getClass().getCanonicalName())
                .commit();
    }

    // The Handler that gets information back from the BluetoothService
    @SuppressLint("HandlerLeak")
    private final Handler mSensorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                            break;
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    processSensorMessage(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void processSensorMessage(String readMessage) {

        String[] parts = readMessage.split(",");

        if (parts.length == 2) {

            try {

                lastY = Double.parseDouble(parts[0]);
                lastZ = Double.parseDouble(parts[1]);

            } catch (Exception e) {

                lastY = 0;
                lastY = 0;

            }

        } else if (parts.length == 3) {// && lecturaActual == ORIENTATION_INDEX) {
            double pitch, roll, yaw;

            try {

                pitch = Double.parseDouble(parts[0]);
                yaw = Double.parseDouble(parts[1]);
                roll = Double.parseDouble(parts[2]);

            } catch (Exception e) {

                pitch = 0;
                yaw = 0;
                roll = 0;

            }

            String msg = pitch + "," + roll + "\r\n";
            sendMessage(msg);
            //lecturaActual = (lecturaActual + 1)%2;
            long timestamp = System.currentTimeMillis();
            EventBus.getDefault().post(new SensorReadingEvent(pitch, roll, lastY, lastZ, 0, timestamp));
        }
        /*else if(parts.length == 3 && lecturaActual == GYROSCOPE_INDEX) {
            double pitch, roll, yaw;
            try {

                pitch = Double.parseDouble(parts[0]);
                yaw = Double.parseDouble(parts[1]);
                roll = Double.parseDouble(parts[2]);

            } catch (Exception e) {

                pitch = 0;
                yaw = 0;
                roll = 0;

            }
            lecturaActual = (lecturaActual + 1)%2;
            magGyro = Math.sqrt(pitch*pitch + yaw*yaw + roll*roll);

        }*/
    }

    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED)
            return;

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    private void sendSensorMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mBluetoothSensorService.getState() != BluetoothSensorService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothSensorService.write(send);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // Initialize the BluetoothService to perform bluetooth connections
        if (mBluetoothService == null) {

            mBluetoothService = new BluetoothService(this, mSensorHandler);

            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(
                    sharedPreferencesManager.getCarSensorMacAddress()
            );

            mBluetoothService.connect(bluetoothDevice);
        }

        if (mBluetoothSensorService == null) {

            mBluetoothSensorService = new BluetoothSensorService(this, mSensorHandler);

            BluetoothDevice sensorDevice = mBluetoothAdapter.getRemoteDevice(
                    sharedPreferencesManager.getHeadSensorMacAddress()
            );

            mBluetoothSensorService.connect(sensorDevice);
        }

        EventBus.getDefault().register(this);

    }

    private void startSensorStreaming() {
        //sendSensorMessage(":80,38,7,255,255,255,255,255,255\n");
        sendSensorMessage(":80,7,255,255,255,255,255,255,255\n");
        sendSensorMessage(":82,25000,-1,0\n");
        sendSensorMessage(":85\n");
    }

    private void stopSensorStreaming() {
        sendSensorMessage(":86\n");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (D) Log.e(TAG, "-- ON STOP --");

        EventBus.getDefault().unregister(this);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }

        if (mBluetoothSensorService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothSensorService.getState() == BluetoothSensorService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothSensorService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (D) Log.e(TAG, "--- ON DESTROY ---");

        // Stop the Bluetooth chat services
        if (mBluetoothService != null)
            mBluetoothService.stop();
        if (mBluetoothSensorService != null) {
            stopSensorStreaming();
            mBluetoothSensorService.stop();
        }

    }

}
