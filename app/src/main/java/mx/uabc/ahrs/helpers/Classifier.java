package mx.uabc.ahrs.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mx.uabc.ahrs.interfaces.DistanceAlgorithm;
import mx.uabc.ahrs.models.DataPoint;

public class Classifier {

    private int K;
    private DistanceAlgorithm distanceAlgorithm;

    private List<DataPoint> listTrainData;

    public Classifier() {

        K = 3;
        distanceAlgorithm = new EuclideanDistance();
        listTrainData = new ArrayList<>();

    }

    private List<Double> calculateDistances(DataPoint point) {

        List<Double> listDistance = new ArrayList<>();

        for (DataPoint dataPoint : listTrainData) {
            double distance = distanceAlgorithm.calculateDistance(point.getX(), point.getY(), point.getZ(),
                    dataPoint.getX(), dataPoint.getY(), dataPoint.getZ());
            listDistance.add(distance);
        }

        return listDistance;
    }

    private int getMaxCategory(HashMap<Integer, Integer> hashMap) {

        Iterator<Map.Entry<Integer, Integer>> iterator = hashMap.entrySet().iterator();
        int maxCategory = Integer.MIN_VALUE;
        int category = -1;

        while (iterator.hasNext()) {

            Map.Entry<Integer, Integer> item = iterator.next();

            if (item.getValue() > maxCategory) {
                category = item.getKey();
            }

        }

        return category;
    }

    public int classifyDataPoint(DataPoint point) {

        HashMap<Integer, Integer> hashMap = new HashMap<>();

        List<Double> listDistance = calculateDistances(point);

        for (int i = 0; i < K; i++) {

            double min = Double.MAX_VALUE;

            int minIndex = -1;

            for (int j = 0; j < listDistance.size(); j++) {

                if (listDistance.get(j) < min) {
                    min = listDistance.get(j);
                    minIndex = j;
                }
            }

            int category = listTrainData.get(minIndex).getSpot();

            if (hashMap.containsKey(category)) {
                hashMap.put(category, hashMap.get(category) + 1);
            } else {
                hashMap.put(category, 1);
            }

            listDistance.set(minIndex, Double.MAX_VALUE);
        }

        return getMaxCategory(hashMap);
    }

    public void addTrainingData(DataPoint dataPoint) {
        listTrainData.add(dataPoint);
    }

}
