package mx.uabc.ahrs.events;

public class SensorReadingEvent {

    private double pitch;
    private double roll;
    private double accelX;
    private double accelY;
    private double accelZ;
    private long timestamp;

    public SensorReadingEvent(double pitch, double roll, long timestamp) {
        this.pitch = pitch;
        this.roll = roll;
        this.accelX = 0;
        this.accelY = 0;
        this.accelZ = 0;
        this.timestamp = timestamp;
    }

    public SensorReadingEvent(double accx, double accy, double accz, long timestamp) {
        this.accelX = accx;
        this.accelY= accy;
        this.accelZ = accz;
        this.timestamp = timestamp;
        this.pitch = 0;
        this.roll = 0;
    }
    public double getAccelX() {
        return accelX;
    }

    public double getAccelY() {
        return accelY;
    }

    public double getAccelZ() {
        return accelZ;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
