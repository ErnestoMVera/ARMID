package mx.uabc.ahrs.helpers;

import mx.uabc.ahrs.interfaces.DistanceAlgorithm;

public class EuclideanDistance implements DistanceAlgorithm {

    @Override
    public double calculateDistance(double x1, double y1, double x2, double y2) {
        double xSquare = Math.pow(x1 - x2, 2);
        double ySquare = Math.pow(y1 - y2, 2);
        return Math.sqrt(xSquare + ySquare);
    }

    @Override
    public double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double xSquare = Math.pow(x1 - x2, 2);
        double ySquare = Math.pow(y1 - y2, 2);
        double zSquare = Math.pow(z1 - z2, 2);
        return Math.sqrt(xSquare + ySquare + zSquare);
    }
}