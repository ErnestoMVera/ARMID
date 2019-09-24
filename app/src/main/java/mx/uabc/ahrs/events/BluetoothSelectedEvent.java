package mx.uabc.ahrs.events;

public class BluetoothSelectedEvent {

    public enum Reference {
        HEAD, CAR
    }

    private String macAddress;
    private Reference reference;

    public String getMacAddress() {
        return macAddress;
    }

    public Reference getReference() {
        return reference;
    }

    public BluetoothSelectedEvent(String macAddress, Reference reference) {
        this.macAddress = macAddress;
        this.reference = reference;
    }
}
