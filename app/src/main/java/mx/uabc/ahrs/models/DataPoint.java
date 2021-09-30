package mx.uabc.ahrs.models;

public class DataPoint {

    private double pitch;
    private double roll;
    private double x;
    private double y;
    private double z;
    private int spot;

    public DataPoint(double pitch, double roll, double y, double z, int spot) {
        this.pitch = pitch;
        this.roll = roll;
        this.spot = spot;
    }
    public DataPoint(double x, double y, double z) {
        this.pitch = 0;
        this.roll = 0;
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public double getX() {
        return x;
    }

    public DataPoint(DataPoint dataPoint) {
        this(dataPoint.getPitch(), dataPoint.getRoll(),
                dataPoint.getY(), dataPoint.getZ(), dataPoint.getSpot());
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public double getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public double getZ() {
        return z;
    }

    public int getSpot() {
        return spot;
    }

    public void setSpot(int spot) {
        this.spot = spot;
    }

    public static String getSpotName(int spot) {

        switch (spot) {
            case 0:
                return "Camino";
            case 1:
                return "Izquierda";
            case 2:
                return "Derecha";
            case 3:
                return "Piernas";
            case 4:
                return "Audio / Clima";
        }

        return "No definido";
    }
}

