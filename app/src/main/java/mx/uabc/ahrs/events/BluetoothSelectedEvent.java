package mx.uabc.ahrs.events;

public class BluetoothSelectedEvent {

    private String macAddress;

    public String getMacAddress() {
        return macAddress;
    }

    public BluetoothSelectedEvent(String macAddress) {
        this.macAddress = macAddress;
    }
}
