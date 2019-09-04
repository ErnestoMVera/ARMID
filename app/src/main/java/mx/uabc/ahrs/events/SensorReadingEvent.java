package mx.uabc.ahrs.events;

public class SensorReadingEvent {

    private double x;
    private double y;
    private double z;
    private long timestamp;

    public SensorReadingEvent(double x, double y, double z, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public double getX() {
        return x;
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
