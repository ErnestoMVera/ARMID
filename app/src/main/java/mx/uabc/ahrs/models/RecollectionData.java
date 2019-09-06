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
    private String comportamientoConduccion;
    private String ejecucionComportamiento;
    private String tareaSecundariaConduccion;

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

    public String getComportamientoConduccion() {
        return comportamientoConduccion;
    }

    public String getEjecucionComportamiento() {
        return ejecucionComportamiento;
    }

    public String getTareaSecundariaConduccion() {
        return tareaSecundariaConduccion;
    }

    public RecollectionData(double pitch, double roll, double yaw, int spot, long timestamp,
                            float speed, double lat, double lng,
                            String comportamientoConduccion, String ejecucionComportamiento,
                            String tareaSecundariaConduccion) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.spot = spot;
        this.timestamp = timestamp;
        this.speed = speed;
        this.lat = lat;
        this.lng = lng;
        this.comportamientoConduccion = comportamientoConduccion;
        this.ejecucionComportamiento = ejecucionComportamiento;
        this.tareaSecundariaConduccion = tareaSecundariaConduccion;
    }
}
