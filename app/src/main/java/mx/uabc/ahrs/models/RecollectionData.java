package mx.uabc.ahrs.models;

public class RecollectionData {


    private int spot;
    private float speed;
    private double lat;
    private double lng;

    public int getSpot() {
        return spot;
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

    public RecollectionData(int spot, float speed, double lat, double lng) {
        this.spot = spot;
        this.speed = speed;
        this.lat = lat;
        this.lng = lng;
    }
}
