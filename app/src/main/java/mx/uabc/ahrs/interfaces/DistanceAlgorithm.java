package mx.uabc.ahrs.interfaces;

public interface DistanceAlgorithm {

    double calculateDistance(double x1, double y1, double x2, double y2);

    double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2);

    double calculateDistance(double pitch1, double roll1, double y1, double z1,
                             double pitch2, double roll2, double y2, double z2);
}
