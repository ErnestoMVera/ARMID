package mx.uabc.ahrs.fragments;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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

public class ClassifierFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private Classifier classifier;

    private Unbinder unbinder;
    private Context mContext;

    @OnClick(R.id.classifier_btn)
    public void classify(View view) {

        Button button = (Button) view;

        int action = isRecording ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isRecording ? "Clasificar" : "Detener";
        button.setText(text);
        isRecording = !isRecording;
    }

    @BindView(R.id.class_label)
    TextView classLabel;
    @BindView(R.id.sample_label)
    TextView sampleLabel;

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        DataPoint dataPoint = new DataPoint(event.getX(), event.getY(), event.getZ(), -1);
        int spot = classifier.classifyDataPoint(dataPoint);
        String s = DataPoint.getSpotName(spot);
        classLabel.setText(s);

    }

    public ClassifierFragment() {
        // Required empty public constructor
    }

    public static ClassifierFragment newInstance(int userId) {

        ClassifierFragment myFragment = new ClassifierFragment();

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

        View view = inflater.inflate(R.layout.fragment_classifier, container, false);
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

        int length = 0;

        try {
            FileReader reader = new FileReader(trainingFile);
            CSVReader csvReader = new CSVReader(reader);

            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {

                length++;

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

        String txt = "Tamaño del dataset: " + length + " muestras";
        sampleLabel.setText(txt);

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
