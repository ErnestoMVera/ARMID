package mx.uabc.ahrs.models;

public class RecollectionData {

    private double pitch;
    private double roll;
    private double yaw;
    private int spot;
    private long timestamp;
    private float speed;
    private double lat;
    private double lng;
    private String activity;
    private String roadType;

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public double getYaw() {
        return yaw;
    }

    public int getSpot() {
        return spot;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getSpeed() {
        return speed;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getActivity() {
        return activity;
    }

    public String getRoadType() {
        return roadType;
    }

    public RecollectionData(double pitch, double roll, double yaw, int spot, long timestamp,
                            float speed, double lat, double lng, String activity, String roadType) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.spot = spot;
        this.timestamp = timestamp;
        this.speed = speed;
        this.lat = lat;
        this.lng = lng;
        this.activity = activity;
        this.roadType = roadType;
    }
}
