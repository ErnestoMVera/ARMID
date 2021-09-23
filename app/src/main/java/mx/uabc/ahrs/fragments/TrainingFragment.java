package mx.uabc.ahrs.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import mehdi.sakout.fancybuttons.FancyButton;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.adapters.TrainingAdapter;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.models.DataPoint;

public class TrainingFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";
    private int selectedIndex;
    private User user;
    private boolean isRecording;
    private ProgressDialog progressDialog;

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        DataPoint dataPoint = new DataPoint(event.getPitch(), event.getRoll(), event.getY(), event.getZ(), selectedIndex);

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> adapter.addItem(selectedIndex, dataPoint));
    }

    @BindView(R.id.listView)
    ListView listView;

    @OnClick(R.id.clear_spot)
    public void clearSpot() {

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> adapter.clearItem(selectedIndex));
    }

    @OnClick(R.id.record_spot)
    public void recordSpot(View view) {

        FancyButton recordBtn = (FancyButton) view;

        int action = isRecording ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isRecording ? "Recolectar" : "Detener";
        recordBtn.setText(text);
        isRecording = !isRecording;

    }

    @OnClick(R.id.load_dataset)
    public void loadDataset() {

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (!trainingFile.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> adapter.clearItems());

        try {
            FileReader reader = new FileReader(trainingFile);
            CSVReader csvReader = new CSVReader(reader);

            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {

                double pitch, roll, y, z;
                int spot;

                pitch = Double.parseDouble(nextRecord[0]);
                roll = Double.parseDouble(nextRecord[1]);
                y = Double.parseDouble(nextRecord[2]);
                z = Double.parseDouble(nextRecord[3]);
                spot = Integer.parseInt(nextRecord[4]);

                DataPoint dataPoint = new DataPoint(pitch, roll, y, z, spot);

                assert getActivity() != null;
                getActivity().runOnUiThread(() -> adapter.addItem(spot, dataPoint));

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

    @OnClick(R.id.save_dataset)
    public void saveDataset() {

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (trainingFile.exists()) {
            trainingFile.delete();
        } else {
            try {
                trainingFile.createNewFile();
            } catch (IOException e) {
                Toast.makeText(mContext, "No se pudo crear el dataset",
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        List<List<DataPoint>> puntos = adapter.getPointsList();

        try {

            FileOutputStream fileOutputStream = new FileOutputStream(trainingFile);

            for (List<DataPoint> list : puntos) {
                for (DataPoint dataPoint : list) {
                    String data = dataPoint.getPitch() + ","
                            + dataPoint.getRoll() + ","
                            + dataPoint.getY() + ","
                            + dataPoint.getZ() + ","
                            + dataPoint.getSpot() + "\n";
                    fileOutputStream.write(data.getBytes());
                }
            }

            fileOutputStream.close();

            Toast.makeText(mContext, "Dataset guardado", Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            Toast.makeText(mContext, "No se encontró el dataset", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(mContext, "No se pudo escribir el dataset", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @OnClick(R.id.upload_dataset)
    public void uploadDataset() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        File trainingFile = new File(mContext.getFilesDir(), user.trainingFilename);

        if (!trainingFile.exists()) {
            Toast.makeText(mContext, "No se encontró el dataset de entrenamiento",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Uri file = Uri.fromFile(trainingFile);

        String[] split = user.name.toLowerCase().split(" ");
        StringBuilder ref = new StringBuilder();

        for (String p : split) {
            ref.append(p).append("_");
        }

        ref.append("training.csv");

        StorageReference trainingRef = storageRef.child("training/" + ref);

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

    private Unbinder unbinder;
    private Context mContext;
    private TrainingAdapter adapter;

    public TrainingFragment() {
        // Required empty public constructor
    }

    public static TrainingFragment newInstance(int userId) {

        TrainingFragment myFragment = new TrainingFragment();

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_training, container, false);
        unbinder = ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    private void initUI() {

        adapter = new TrainingAdapter(5);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedIndex = i;
            assert getActivity() != null;
            getActivity().runOnUiThread(() -> adapter.setSelectedIndex(i));
        });

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
