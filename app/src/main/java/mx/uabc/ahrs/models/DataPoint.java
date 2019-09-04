package mx.uabc.ahrs.models;

public class DataPoint {

    private double x;
    private double y;
    private double z;
    private int spot;

    public DataPoint(double x, double y, double z, int spot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.spot = spot;
    }

    public DataPoint(DataPoint dataPoint) {
        this(dataPoint.getX(), dataPoint.getY(), dataPoint.getZ(), dataPoint.getSpot());
    }

    public double getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
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
                return "Parabrisas";
            case 1:
                return "Retrovisor central";
            case 2:
                return "Retrovisor izquierdo";
            case 3:
                return "Retrovisor derecho";
            case 4:
                return "Dashboard";
            case 5:
                return "Radio";
            case 6:
                return "Copiloto";
        }

        return "No definido";
    }
}

