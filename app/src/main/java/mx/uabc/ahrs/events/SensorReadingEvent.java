package mx.uabc.ahrs.events;

public class SensorReadingEvent {

    private double pitch;
    private double roll;
    private double y;
    private double z;
    private long timestamp;

    public SensorReadingEvent(double pitch, double roll, double y, double z, long timestamp) {
        this.pitch = pitch;
        this.roll = roll;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
