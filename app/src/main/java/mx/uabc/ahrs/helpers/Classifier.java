package mx.uabc.ahrs.helpers;

import java.util.List;

import mx.uabc.ahrs.models.DataPoint;

/**
 * El objeto classifier se usa en tres clases,
 * ClassifierFragment, RecollectionFragment y Validation Fragment.
 */
public interface Classifier {
    // Metodo para clasificar.
    int classifyDataPoint(DataPoint point);
    /**
     * Metodo para entrenar, en el caso de no usar el algoritmo KNN,
     * este metodo se puede sobreescribir para entrenar el modelo (eg, redes neuronales, SVM o Naive bayes)
     **/
    void addTrainingData(List<DataPoint> dataPoints);
}
