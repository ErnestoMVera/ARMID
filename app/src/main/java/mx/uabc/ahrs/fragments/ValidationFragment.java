package mx.uabc.ahrs.fragments;


import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVReader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.helpers.Classifier;
import mx.uabc.ahrs.models.DataPoint;

public class ValidationFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private Classifier classifier;
    private Location lastLocation;

    private Unbinder unbinder;
    private Context mContext;
    private FusedLocationProviderClient fusedLocationClient;
    private FileOutputStream fileOutputStream;

    private List<Integer> spots = new ArrayList<>();

    @BindView(R.id.puntosSeleccionados)
    TextView puntosSeleccionadosTextView;
    @BindView(R.id.detalles)
    TextView detallesTextView;
    @BindView(R.id.classifier_btn)
    Button classifierButton;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            if (locationResult == null) {
                return;
            }

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                lastLocation = locationList.get(locationList.size() - 1);
            }
        }
    };

    @OnClick({R.id.s0, R.id.s1, R.id.s2, R.id.s3, R.id.s4})
    public void onChipClicked(View view) {

        int spot = 0;

        switch (view.getId()) {
            case R.id.s0:
                spot = 0;
                break;
            case R.id.s1:
                spot = 1;
                break;
            case R.id.s2:
                spot = 2;
                break;
            case R.id.s3:
                spot = 3;
                break;
            case R.id.s4:
                spot = 4;
                break;
        }

        boolean found = false;
        int index = 0;

        for (int s : spots) {
            if (spot == s) {
                found = true;
                break;
            } else {
                index++;
            }
        }

        if (!found)
            spots.add(spot);
        else {
            spots.remove(index);
        }

        updateTextView();
    }

    @OnClick(R.id.classifier_btn)
    public void classify(View view) {

        Button button = (Button) view;

        int action = isRecording ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isRecording ? "Clasificar" : "Detener";
        button.setText(text);
        isRecording = !isRecording;

        if (isRecording) {

            String[] split = user.name.toLowerCase().split(" ");
            StringBuilder ref = new StringBuilder();

            for (String p : split) {
                ref.append(p).append("_");
            }

            ref.append(UUID.randomUUID().toString());
            ref.append("_");
            ref.append("validation.csv");

            String fileName = ref.toString();

            File validationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            if (!validationFile.exists()) {
                try {
                    validationFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {

                fileOutputStream = new FileOutputStream(validationFile);

                String header = "";
                for (int spot : spots) {
                    header = header.concat(spot + ",");
                }

                header = header.concat("timestamp,spot,speed\n");

                fileOutputStream.write(header.getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            }

            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);

        } else {

            fusedLocationClient.removeLocationUpdates(locationCallback);

            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            updateDetallesTextView(false, 0, 0.0);
        }
    }

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        if (lastLocation == null || fileOutputStream == null)
            return;

        DataPoint dataPoint = new DataPoint(event.getX(), event.getY(), event.getZ(), -1);
        int predictedSpot = classifier.classifyDataPoint(dataPoint);

        double speed = lastLocation.getSpeed() * 3.6;

        String text = event.getTimestamp() + "," + predictedSpot + "," + speed + "\n";

        try {
            fileOutputStream.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateDetallesTextView(true, predictedSpot, speed);
    }

    public ValidationFragment() {
        // Required empty public constructor
    }

    private void updateDetallesTextView(boolean isRecording, int spot, double speed) {

        String text = isRecording ? DataPoint.getSpotName(spot) + "\n" + speed : "";
        assert getActivity() != null;
        getActivity().runOnUiThread(() -> detallesTextView.setText(text));

    }

    private void updateTextView() {

        String text = "";
        int pos = 1;

        for (int s : spots) {
            String spotName = pos + ".- " + DataPoint.getSpotName(s) + "\n";
            text = text.concat(spotName);
            pos++;
        }

        assert getActivity() != null;
        String finalText = text;
        getActivity().runOnUiThread(() -> {
            classifierButton.setEnabled(spots.size() > 1);
            puntosSeleccionadosTextView.setText(finalText);
        });
    }

    private LocationRequest createLocationRequest() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;

    }

    public static ValidationFragment newInstance(int userId) {

        ValidationFragment myFragment = new ValidationFragment();

        Bundle args = new Bundle();
        args.putInt(USER_ID_PARAM, userId);
        myFragment.setArguments(args);

        return myFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        int userId = args.getInt(USER_ID_PARAM);
        user = DatabaseManager.getInstance(mContext).getUserDao().loadById(userId);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_validation, container, false);
        unbinder = ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    private void initUI() {

        classifier = new Classifier();

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (!trainingFile.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<DataPoint> dataPointList = new ArrayList<>();

        try {
            FileReader reader = new FileReader(trainingFile);
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

            classifier.addTrainingData(dataPointList);

        } catch (FileNotFoundException e) {
            Toast.makeText(mContext,
                    "No se encontró el dataset", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(mContext,
                    "No se pudo leer el dataset", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        mContext = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (isRecording)
            EventBus.getDefault().post(new SensorStreamingEvent(SensorStreamingEvent.STOP));

        super.onDestroy();
    }

}
