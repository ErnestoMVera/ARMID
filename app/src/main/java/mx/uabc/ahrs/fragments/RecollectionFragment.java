package mx.uabc.ahrs.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
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
import mx.uabc.ahrs.helpers.Utils;
import mx.uabc.ahrs.models.DataPoint;
import mx.uabc.ahrs.models.RecollectionData;

public class RecollectionFragment extends Fragment {

    private enum Comportamiento {
        LINEA_RECTA, VUELTA_IZQUIERDA, VUELTA_DERECHA, SEMAFORO, ESTACIONAMIENTO,
        CAMBIO_CARRIL_IZQUIERDA, CAMBIO_CARRIL_DERECHA
    }

    private enum TareaSecundaria {
        SIN_COPILOTO, CON_COPILOTO
    }

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private Classifier classifier;
    private Location lastLocation;
    private String comportamiento;
    private String tareaSecundaria;

    private Unbinder unbinder;
    private Context mContext;
    private RecollectionAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;
    private FileOutputStream fileOutputStream;

    private ProgressDialog progressDialog;

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

    private void setFabBackgroundToDefault(List<FloatingActionButton> fabs) {
        for (FloatingActionButton fab : fabs) {
            fab.setBackgroundTintList(ColorStateList
                    .valueOf(Color.parseColor("#536DFE")));
        }
    }

    private void setFabBackgroundToSelected(FloatingActionButton fab) {
        fab.setBackgroundTintList(ColorStateList
                .valueOf(Color.parseColor("#607D8B")));
    }

    @OnClick({R.id.action_straight, R.id.action_turn_left, R.id.action_turn_right,
            R.id.action_traffic_light, R.id.action_parking, R.id.action_change_left,
            R.id.action_change_right})
    public void onComportamientoClicked(View view) {

        setFabBackgroundToDefault(comportamientosFABs);
        setFabBackgroundToSelected((FloatingActionButton) view);

        switch (view.getId()) {
            case R.id.action_straight:
                comportamiento = Comportamiento.LINEA_RECTA.toString();
                break;
            case R.id.action_turn_left:
                comportamiento = Comportamiento.VUELTA_IZQUIERDA.toString();
                break;
            case R.id.action_turn_right:
                comportamiento = Comportamiento.VUELTA_DERECHA.toString();
                break;
            case R.id.action_traffic_light:
                comportamiento = Comportamiento.SEMAFORO.toString();
                break;
            case R.id.action_parking:
                comportamiento = Comportamiento.ESTACIONAMIENTO.toString();
                break;
            case R.id.action_change_left:
                comportamiento = Comportamiento.CAMBIO_CARRIL_IZQUIERDA.toString();
                break;
            case R.id.action_change_right:
                comportamiento = Comportamiento.CAMBIO_CARRIL_DERECHA.toString();
                break;
        }
    }

    @OnClick({R.id.sin_copiloto, R.id.con_copiloto})
    public void onTareaSecundariaClicked(View view) {

        setFabBackgroundToDefault(tareaSecundariaFABs);
        setFabBackgroundToSelected((FloatingActionButton) view);

        if (view.getId() == R.id.sin_copiloto) {
            tareaSecundaria = TareaSecundaria.SIN_COPILOTO.toString();
        } else {
            tareaSecundaria = TareaSecundaria.CON_COPILOTO.toString();
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

            if (!datasetFile.exists()) {
                try {
                    datasetFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {

                fileOutputStream = new FileOutputStream(datasetFile, true);

                String header = "timestamp,x,y,spot,speed,lat,lng,comportamiento,tarea_secundaria\n";

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

    @BindViews({R.id.action_straight, R.id.action_turn_left, R.id.action_turn_right,
            R.id.action_traffic_light, R.id.action_parking, R.id.action_change_left,
            R.id.action_change_right})
    List<FloatingActionButton> comportamientosFABs;

    @BindViews({R.id.sin_copiloto, R.id.con_copiloto})
    List<FloatingActionButton> tareaSecundariaFABs;

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        if (lastLocation == null || fileOutputStream == null)
            return;

        DataPoint dataPoint = new DataPoint(event.getX(), event.getY(), event.getZ(), -1);
        int spot = classifier.classifyDataPoint(dataPoint);

        RecollectionData data = new RecollectionData(spot, lastLocation.getSpeed(),
                lastLocation.getLatitude(), lastLocation.getLongitude()
        );

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> adapter.addItem(data));

        String toSave = mContext.getString(R.string.recollection_template, event.getTimestamp(),
                event.getX(), event.getY(), spot, lastLocation.getSpeed(), lastLocation.getLatitude(),
                lastLocation.getLongitude(), comportamiento, tareaSecundaria);

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

        initButtons();

        adapter = new RecollectionAdapter();
        listView.setAdapter(adapter);

        classifier = new Classifier();

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (!trainingFile.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<DataPoint> trainingDataSet = Utils.getTrainingDataSet(trainingFile);
        classifier.addTrainingData(trainingDataSet);

    }

    private void initButtons() {

        FloatingActionButton fab1 = comportamientosFABs.get(0);
        FloatingActionButton fab2 = tareaSecundariaFABs.get(0);

        setFabBackgroundToSelected(fab1);
        setFabBackgroundToSelected(fab2);

        comportamiento = Comportamiento.LINEA_RECTA.toString();
        tareaSecundaria = TareaSecundaria.SIN_COPILOTO.toString();
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
