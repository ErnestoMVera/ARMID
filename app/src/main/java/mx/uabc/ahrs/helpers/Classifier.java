package mx.uabc.ahrs.helpers;

import java.util.List;

import mx.uabc.ahrs.models.DataPoint;

/**
 * El objeto classifier se usa en tres clases,
 * ClassifierFragment, RecollectionFragment y Validation Fragment.
 *
 */
public interface Classifier {
    int classifyDataPoint(DataPoint point);
    void addTrainingData(List<DataPoint> dataPoints);
}
