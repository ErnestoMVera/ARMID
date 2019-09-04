package mx.uabc.ahrs.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static final String SAMPLING_RATE = "samplingRate";
    private static final String BT_DEVICE_NAME = "btDeviceName";

    private static volatile SharedPreferencesManager instance;
    private SharedPreferences sharedPref;

    public static SharedPreferencesManager getInstance(Context context) {

        if (instance == null) {
            instance = new SharedPreferencesManager();
            instance.configSessionUtils(context);
        }

        return instance;
    }

    private void configSessionUtils(Context context) {
        sharedPref = context.getSharedPreferences("AppPreferences", Activity.MODE_PRIVATE);
    }

    public void setSamplingRate(int samplingRate) {
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putInt(SAMPLING_RATE, samplingRate);
        sharedPrefEditor.apply();
    }

    public int getSamplingRate() {
        return sharedPref.getInt(SAMPLING_RATE, 10);
    }

    public String getBtDeviceName() {
        return sharedPref.getString(BT_DEVICE_NAME, "YostLabsMBT");
    }

}
