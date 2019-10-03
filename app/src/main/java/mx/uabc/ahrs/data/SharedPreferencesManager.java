package mx.uabc.ahrs.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static final String HEAD_SENSOR_MAC_ADDRESS = "HEAD_SENSOR_MAC_KEY";
    private static final String CAR_SENSOR_MAC_ADDRESS = "CAR_SENSOR_MAC_KEY";

    private static volatile SharedPreferencesManager instance;
    private SharedPreferences mSharedPref;

    private SharedPreferencesManager(Context context) {

        mSharedPref = context.getSharedPreferences(context.getPackageName(),
                Activity.MODE_PRIVATE);

    }

    public String getHeadSensorMacAddress() {
        return mSharedPref.getString(HEAD_SENSOR_MAC_ADDRESS, "");
    }

    public void setHeadSensorMacAddress(String headSensorMacAddress) {
        SharedPreferences.Editor prefsEditor = mSharedPref.edit();
        prefsEditor.putString(HEAD_SENSOR_MAC_ADDRESS, headSensorMacAddress);
        prefsEditor.apply();
    }

    public String getCarSensorMacAddress() {
        return mSharedPref.getString(CAR_SENSOR_MAC_ADDRESS, "");
    }

    public void setCarSensorMacAddress(String carSensorMacAddress) {
        SharedPreferences.Editor prefsEditor = mSharedPref.edit();
        prefsEditor.putString(CAR_SENSOR_MAC_ADDRESS, carSensorMacAddress);
        prefsEditor.apply();
    }


    public static SharedPreferencesManager getInstance(Context context) {

        if (instance == null)
            instance = new SharedPreferencesManager(context);

        return instance;
    }

}
