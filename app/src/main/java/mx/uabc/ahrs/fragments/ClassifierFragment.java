package mx.uabc.ahrs.fragments;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.helpers.Classifier;
import mx.uabc.ahrs.helpers.Utils;
import mx.uabc.ahrs.models.DataPoint;

public class ClassifierFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private Classifier classifier;

    private Unbinder unbinder;
    private Context mContext;

    private int selectedSpot;
    private int[][] confusionMatrix = new int[5][5];

    private int readings, correctReadings, incorrectReadings;

    @OnClick(R.id.classifier_btn)
    public void classify(View view) {

        Button button = (Button) view;

        int action = isRecording ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isRecording ? "Clasificar" : "Detener";
        button.setText(text);
        isRecording = !isRecording;
    }

    TextView yawTextView;
    @BindView(R.id.lecturas)
    TextView lecturas;
    @BindView(R.id.lecturasCorrectas)
    TextView lecturasCorrectas;
    @BindView(R.id.lecturasIncorrectas)
    TextView lecturasIncorrectas;
    @BindView(R.id.precision)
    TextView precision;

    @BindViews({R.id.s0_row, R.id.s1_row, R.id.s2_row, R.id.s3_row, R.id.s4_row})
    List<TableRow> rowsList;

    @BindViews({R.id.s0_tag, R.id.s1_tag, R.id.s2_tag, R.id.s3_tag, R.id.s4_tag})
    List<TextView> tagsList;

    @OnClick({R.id.s0_tag, R.id.s1_tag, R.id.s2_tag, R.id.s3_tag, R.id.s4_tag})
    public void onTagClicked(View view) {

        switch (view.getId()) {
            case R.id.s0_tag:
                selectedSpot = 0;
                break;
            case R.id.s1_tag:
                selectedSpot = 1;
                break;
            case R.id.s2_tag:
                selectedSpot = 2;
                break;
            case R.id.s3_tag:
                selectedSpot = 3;
                break;
            case R.id.s4_tag:
                selectedSpot = 4;
                break;
        }

        for (TextView textView : tagsList) {
            textView.setTextColor(mContext.getResources().getColor(R.color.textColorPrimary));
        }

        TextView selectedTextView = (TextView) view;
        selectedTextView.setTextColor(mContext.getResources().getColor(R.color.colorAccent));
    }

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        readings++;

        DataPoint dataPoint = new DataPoint(event.getX(), event.getY(), event.getZ(), -1);
        int predictedSpot = classifier.classifyDataPoint(dataPoint);

        if (predictedSpot == selectedSpot)
            correctReadings++;
        else
            incorrectReadings++;

        confusionMatrix[selectedSpot][predictedSpot]++;

        updateViews(selectedSpot, predictedSpot);
    }

    @SuppressLint("SetTextI18n")
    private void updateViews(int selectedSpot, int predictedSpot) {

        long ts = System.currentTimeMillis();
        int value = confusionMatrix[selectedSpot][predictedSpot];

        TextView location = (TextView) rowsList
                .get(selectedSpot)
                .getChildAt(predictedSpot + 1);

        float acc = (correctReadings * 100) / readings;

        assert getActivity() != null;
        getActivity().runOnUiThread(() -> {
            location.setText(String.valueOf(value));
            lecturas.setText("Lecturas: " + readings);
            lecturasCorrectas.setText("Clasificadas correctamente: " + correctReadings);
            lecturasIncorrectas.setText("Clasificadas incorrectamente: " + incorrectReadings);
            precision.setText("Precisión: " + acc + " %");
        });

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

        tagsList.get(0).callOnClick();

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
