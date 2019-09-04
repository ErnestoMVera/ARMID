package mx.uabc.ahrs.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opencsv.CSVReader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.adapters.RecollectionAdapter;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.helpers.Classifier;
import mx.uabc.ahrs.models.DataPoint;
import mx.uabc.ahrs.models.RecollectionData;

public class RecollectionFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private Classifier classifier;
    private Location lastLocation;
    private RoadType roadType;

    private Unbinder unbinder;
    private Context mContext;
    private RecollectionAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;
    private FileOutputStream fileOutputStream;

    private ProgressDialog progressDialog;

    private enum RoadType {
        Residencial, Bulevard
    }

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

    @OnClick({R.id.fab_home, R.id.fab_road})
    public void changeRoadType(View view) {
        switch (view.getId()) {
            case R.id.fab_home:
                roadType = RoadType.Residencial;
                break;
            case R.id.fab_road:
                roadType = RoadType.Bulevard;
                break;
        }
    }

    @OnClick(R.id.record)
    public void record(View view) {

        Button button = (Button) view;

        int action = isRecording ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isRecording ? "Recolectar" : "Detener";
        button.setText(text);
        isRecording = !isRecording;

        if (isRecording) {

            File datasetFile = new File(mContext.getFilesDir(), user.datasetFilename);

            if (datasetFile.exists()) {
                datasetFile.delete();
            }

            try {
                datasetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                fileOutputStream = new FileOutputStream(datasetFile);
            } catch (FileNotFoundException e) {
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
        }
    }

    @OnClick(R.id.upload)
    public void upload() {

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        File dataset = new File(mContext.getFilesDir(), user.datasetFilename);

        if (!dataset.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Uri file = Uri.fromFile(dataset);

        String[] split = user.name.toLowerCase().split(" ");
        StringBuilder ref = new StringBuilder();

        for (String p : split) {
            ref.append(p).append("_");
        }

        ref.append("dataset.csv");

        StorageReference trainingRef = storageRef.child("dataset/" + ref);

        progressDialog = ProgressDialog.show(mContext, "",
                "Subiendo archivo...",
                true, false);

        UploadTask uploadTask = trainingRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(exception -> {
            Toast.makeText(mContext,
                    exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        })
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(mContext,
                            "Dataset subido exitosamente", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }

    @BindView(R.id.listView)
    ListView listView;

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        if (lastLocation == null || fileOutputStream == null)
            return;

        DataPoint dataPoint = new DataPoint(event.getX(), event.getY(), event.getZ(), -1);
        int spot = classifier.classifyDataPoint(dataPoint);

        RecollectionData data = new RecollectionData(event.getX(), event.getY(), event.getZ(),
                spot, event.getTimestamp(),
                lastLocation.getSpeed(), lastLocation.getLatitude(), lastLocation.getLongitude(),
                "Manejando", roadType.toString());

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> adapter.addItem(data));

        String toSave = event.getX() + "," + event.getY() + "," + event.getZ() + "," + spot + ","
                + event.getTimestamp() + "," + lastLocation.getSpeed() + ","
                + lastLocation.getLatitude() + "," + lastLocation.getLongitude() + ","
                + "Manejando" + "," + roadType.toString() + "\n";

        try {

            fileOutputStream.write(toSave.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public RecollectionFragment() {
        // Required empty public constructor
    }

    public static RecollectionFragment newInstance(int userId) {

        RecollectionFragment myFragment = new RecollectionFragment();

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
        roadType = RoadType.Residencial;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recollection, container, false);
        unbinder = ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    private void initUI() {

        adapter = new RecollectionAdapter();
        listView.setAdapter(adapter);

        classifier = new Classifier();

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (!trainingFile.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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

                classifier.addTrainingData(dataPoint);
            }

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

    private LocationRequest createLocationRequest() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;

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
    public void onAttach(Context context) {
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
