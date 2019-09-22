package mx.uabc.ahrs.dialogs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mx.uabc.ahrs.R;
import mx.uabc.ahrs.adapters.SettingsAdapter;
import mx.uabc.ahrs.events.BluetoothSelectedEvent;
import mx.uabc.ahrs.models.Setting;

public class BluetoothDevicesDialogFragment extends DialogFragment {

    private SettingsAdapter adapter;

    public BluetoothDevicesDialogFragment() {
        //Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_devices_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = view.findViewById(R.id.listView);
        adapter = new SettingsAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            Setting setting = adapter.getItem(i);
            EventBus.getDefault()
                    .post(new BluetoothSelectedEvent(setting.getValue()));
            dismiss();
        });
        fetchDevices();
    }

    private void fetchDevices() {

        List<Setting> settingList = new ArrayList<>();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, set the device mac address string as needed
        //(for now we assume the only paired device is the 3-Space sensor)
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().toLowerCase().contains("yost")) {
                settingList.add(new Setting(device.getName(), device.getAddress()));
            }
        }

        adapter.addItems(settingList);

        //Stop discovery if it is enabled
        mBluetoothAdapter.cancelDiscovery();
    }
}
