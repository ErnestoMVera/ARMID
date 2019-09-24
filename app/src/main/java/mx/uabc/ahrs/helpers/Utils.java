package mx.uabc.ahrs.helpers;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mx.uabc.ahrs.models.DataPoint;

public class Utils {

    public static long hertzToMilliseconds(int hertz) {
        return (long) ((1d / hertz) * 1000d);
    }

    public static List<DataPoint> getTrainingDataSet(File trainingDateSetFile) {

        List<DataPoint> dataPointList = new ArrayList<>();

        try {

            FileReader reader = new FileReader(trainingDateSetFile);
            CSVReader csvReader = new CSVReader(reader);

            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {

                double x, y, z;
                int spot;

                x = Double.parseDouble(nextRecord[0]);
                y = Double.parseDouble(nextRecord[1]);
                z = Double.parseDouble(nextRecord[2]);
                spot = Integer.parseInt(nextRecord[3]);

                DataPoint dataPoint = new DataPoint(x, y, z, spot);

                dataPointList.add(dataPoint);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataPointList;

    }
}
