package mx.uabc.ahrs.helpers;

import mx.uabc.ahrs.interfaces.DistanceAlgorithm;

public class ChebyshevDistance implements DistanceAlgorithm {

    @Override
    public double calculateDistance(double x1, double y1, double x2, double y2) {
        double xi = Math.abs(x1 - x2);
        double xj = Math.abs(y1 - y2);
        return Math.max(xi, xj);
    }

    @Override
    public double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return 0;
    }
}
