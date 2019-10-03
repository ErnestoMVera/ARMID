package mx.uabc.ahrs;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.data.SharedPreferencesManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.fragments.ClassifierFragment;
import mx.uabc.ahrs.fragments.RecollectionFragment;
import mx.uabc.ahrs.fragments.TrainingFragment;
import mx.uabc.ahrs.fragments.ValidationFragment;
import mx.uabc.ahrs.services.BluetoothSensorService;
import mx.uabc.ahrs.services.BluetoothService;

public class SensorActivity extends AppCompatActivity {

    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String USER_ID_PARAM = "userIdParam";

    private int userId;
    //private long sensorDelay;
    private double lastY, lastZ;
    private boolean isConnected = true;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;
    private BluetoothSensorService mBluetoothSensorService = null;

    private SharedPreferencesManager sharedPreferencesManager;


    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            sendSensorMessage(":7\n");

            handler.postDelayed(this, 20);
        }
    };

    private void processSensorMessage(String readMessage) {

        String[] parts = readMessage.split(",");

        if (parts.length == 2) {
            lastY = Double.parseDouble(parts[0]);
            lastZ = Double.parseDouble(parts[1]);
        } else if (parts.length == 3) {
            double pitch = Double.parseDouble(parts[0]);
            double yaw = Double.parseDouble(parts[1]);
            double roll = Double.parseDouble(parts[2]);
            sendLastLecture(pitch, roll);
            long timestamp = System.currentTimeMillis();
            EventBus.getDefault().post(new SensorReadingEvent(pitch, roll, lastY, lastZ, timestamp));
        }
    }

    private void sendLastLecture(double pitch, double roll) {

        if (isConnected) {
            String msg = pitch + "," + roll + "\r\n";
            sendMessage(msg);
        }
    }

    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void processMessage(String text) {

        String[] jsons = text.split("\r\n");

        if (jsons.length > 0) {

            String tmp = jsons[0].trim();

            try {
                JSONObject jsonObject = new JSONObject(tmp);
                lastY = jsonObject.getDouble("Y");
                lastZ = jsonObject.getDouble("Z");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @Subscribe
    public void onSensorStreamingEvent(SensorStreamingEvent sensorStreamingEvent) {

        if (sensorStreamingEvent.getAction() == SensorStreamingEvent.START) {
            handler.post(runnable);
        } else if (sensorStreamingEvent.getAction() == SensorStreamingEvent.STOP) {
            handler.removeCallbacks(runnable);
        }

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

//        sensorDelay = Utils
//                .hertzToMilliseconds(sharedPreferencesManager.getSamplingRate());

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

    // The Handler that gets information back from the BluetoothService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            isConnected = true;
//                            mTitle.setText(R.string.title_connected_to);
//                            mTitle.append(mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
//                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                            isConnected = false;
                        case BluetoothService.STATE_NONE:
                            isConnected = false;
//                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    processMessage(readMessage);
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

    private void startBT() {

        BluetoothDevice sensorDevice = mBluetoothAdapter.getRemoteDevice(
                sharedPreferencesManager.getHeadSensorMacAddress()
        );
        mBluetoothSensorService.connect(sensorDevice);

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(
                sharedPreferencesManager.getCarSensorMacAddress()
        );
        mBluetoothService.connect(bluetoothDevice);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (D) Log.e(TAG, "++ ON START ++");

        // Initialize the BluetoothService to perform bluetooth connections
        if (mBluetoothService == null)
            mBluetoothService = new BluetoothService(this, mSensorHandler);
        if (mBluetoothSensorService == null)
            mBluetoothSensorService = new BluetoothSensorService(this, mSensorHandler);

        EventBus.getDefault().register(this);

        startBT();
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
        // Stop the Bluetooth chat services
        if (mBluetoothService != null) mBluetoothService.stop();
        if (mBluetoothSensorService != null) {
            mBluetoothSensorService.stop();
        }
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

}
