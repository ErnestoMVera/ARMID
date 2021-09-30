package mx.uabc.ahrs.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import mehdi.sakout.fancybuttons.FancyButton;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.ControllerEvent;
import mx.uabc.ahrs.events.SensorReadingEvent;
import mx.uabc.ahrs.events.SensorStreamingEvent;
import mx.uabc.ahrs.helpers.Classifier;
import mx.uabc.ahrs.helpers.Utils;
import mx.uabc.ahrs.models.DataPoint;

import static android.view.KeyEvent.KEYCODE_BUTTON_B;
import static android.view.KeyEvent.KEYCODE_BUTTON_Y;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

public class ValidationFragment extends Fragment {

    private static final String USER_ID_PARAM = "userIdParam";

    private User user;
    private boolean isRecording;
    private boolean isClassifying;
    private Classifier classifier;

    private Unbinder unbinder;
    private Context mContext;
    private FileOutputStream fileOutputStream;

    private int selectedSpot;

    @BindView(R.id.selected_area)
    TextView selectedArea;
    @BindView(R.id.classified_area)
    TextView classifiedArea;

    @BindView(R.id.classify_button)
    FancyButton classifyButton;
    @BindView(R.id.save_button)
    FancyButton saveButton;

    @OnClick(R.id.classify_button)
    public void startStopReadings() {

        int action = isClassifying ? SensorStreamingEvent.STOP : SensorStreamingEvent.START;
        EventBus.getDefault().post(new SensorStreamingEvent(action));
        String text = isClassifying ? "Clasificar" : "Detener";
        classifyButton.setText(text);
        isClassifying = !isClassifying;
    }

    @OnClick(R.id.save_button)
    public void saveReadings() {

        String text = isRecording ? "Guardar" : "Detener";
        saveButton.setText(text);
        isRecording = !isRecording;

        if (isRecording) {

            String[] split = user.name.toLowerCase().split(" ");
            StringBuilder ref = new StringBuilder();

            for (String p : split) {
                ref.append(p).append("_");
            }

            ref.append(System.currentTimeMillis()).append("_validation.csv");

            String fileName = ref.toString();

            File validationFile =
                    new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);

            if (!validationFile.exists()) {
                try {
                    validationFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {

                fileOutputStream = new FileOutputStream(validationFile);
                fileOutputStream.write("timestamp,selected,detected\n".getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {

            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe
    public void onControllerEvent(ControllerEvent event) {

        switch (event.getKeyCode()) {
            case KEYCODE_DPAD_LEFT:
                selectedSpot = 1;
                break;
            case KEYCODE_DPAD_RIGHT:
                selectedSpot = 2;
                break;
            case KEYCODE_DPAD_UP:
                selectedSpot = 0;
                break;
            case KEYCODE_BUTTON_B:
                selectedSpot = 3;
                break;
            case KEYCODE_BUTTON_Y:
                selectedSpot = 4;
                break;
        }

        if (getActivity() != null)
            getActivity().runOnUiThread(() -> selectedArea.
                    setText(DataPoint.getSpotName(selectedSpot)));
    }

    @Subscribe
    public void onSensorReadingEvent(SensorReadingEvent event) {

        DataPoint dataPoint = new DataPoint(event.getPitch(), event.getRoll(),
                0, 0, -1);
        int predictedSpot = classifier.classifyDataPoint(dataPoint);

        String text = mContext.getString(R.string.validation_template,
                event.getTimestamp(), selectedSpot, predictedSpot);

        if (getActivity() != null)
            getActivity().runOnUiThread(() -> classifiedArea
                    .setText(DataPoint.getSpotName(predictedSpot)));

        if (isRecording)
            try {
                if (fileOutputStream != null)
                    fileOutputStream.write(text.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public ValidationFragment() {
        // Required empty public constructor
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

        List<DataPoint> trainingDataSetList = Utils.getTrainingDataSet(trainingFile);
        classifier.addTrainingData(trainingDataSetList);
        selectedSpot = 0;
        if (getActivity() != null)
            getActivity().runOnUiThread(() -> selectedArea
                    .setText(DataPoint.getSpotName(selectedSpot)));
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
