package mx.uabc.ahrs.helpers;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mx.uabc.ahrs.interfaces.DistanceAlgorithm;
import mx.uabc.ahrs.models.DataPoint;

public class KnnClassifier implements Classifier {
    private int K;
    private DistanceAlgorithm distanceAlgorithm;

    private List<DataPoint> listTrainData;

    public KnnClassifier() {

        K = 3;
        distanceAlgorithm = new EuclideanDistance();
        listTrainData = new ArrayList<>();

    }

    private List<Double> calculateDistances(DataPoint point) {

        List<Double> listDistance = new ArrayList<>();

        double pitch1 = point.getPitch();
        double roll1 = point.getRoll();
        double y1 = point.getY();
        double z1 = point.getZ();

        for (DataPoint dataPoint : listTrainData) {

            double pitch2 = dataPoint.getPitch();
            double roll2 = dataPoint.getRoll();
            double y2 = dataPoint.getY();
            double z2 = dataPoint.getZ();

            double distance = distanceAlgorithm
                    .calculateDistance(pitch1, roll1, y1, z1,
                            pitch2, roll2, y2, z2);

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

        @SuppressLint("UseSparseArrays") HashMap<Integer, Integer> hashMap = new HashMap<>();

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

    public void addTrainingData(List<DataPoint> dataPoints) {
        listTrainData.addAll(dataPoints);
        //calculateMeanAndStdDeviation();
    }

//    private void calculateMeanAndStdDeviation() {
//
//        double[] xValues = new double[listTrainData.size()];
//        double[] yValues = new double[listTrainData.size()];
//        double[] zValues = new double[listTrainData.size()];
//
//        for (int i = 0; i < listTrainData.size(); i++) {
//            DataPoint d = listTrainData.get(i);
//            xValues[i] = d.getX();
//            yValues[i] = d.getY();
//            zValues[i] = d.getZ();
//        }
//
//        xMean = StatUtils.mean(xValues);
//        yMean = StatUtils.mean(yValues);
//        zMean = StatUtils.mean(zValues);
//
//        StandardDeviation sd
//                = new StandardDeviation(false);
//
//        xSD = sd.evaluate(xValues);
//        ySD = sd.evaluate(yValues);
//        zSD = sd.evaluate(zValues);
//    }
}

