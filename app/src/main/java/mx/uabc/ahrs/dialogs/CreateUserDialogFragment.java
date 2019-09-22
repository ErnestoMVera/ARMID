package mx.uabc.ahrs.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.divyanshu.draw.widget.DrawView;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import mx.uabc.ahrs.R;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.CreateUserEvent;

public class CreateUserDialogFragment extends DialogFragment {

    private Unbinder unbinder;
    private Context mContext;
    private String selectedGender;

    public CreateUserDialogFragment() {
    }

    @OnClick(R.id.clear_btn)
    public void clearDrawView() {
        drawView.clearCanvas();
    }

    @OnClick(R.id.add_user_btn)
    public void addUser() {

        String name = nameEditText.getEditableText().toString();
        String age = ageEditText.getEditableText().toString();
        int ageNumber;

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(mContext, "El nombre es requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(age)) {
            Toast.makeText(mContext, "La edad es requerida", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedGender)) {
            Toast.makeText(mContext, "El género es requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        ageNumber = Integer.parseInt(age);

        User user = new User(name, ageNumber, selectedGender);
        EventBus.getDefault().post(new CreateUserEvent(user, drawView.getBitmap()));
        dismiss();
    }

    @BindView(R.id.name)
    EditText nameEditText;
    @BindView(R.id.age)
    EditText ageEditText;
    @BindView(R.id.radio_group)
    RadioGroup radioGroup;
    @BindView(R.id.draw_view)
    DrawView drawView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_user_dialog, container);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        assert getDialog() != null;
        getDialog().setTitle("Datos del sujeto");

        radioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            if (i == R.id.radio_masculino) {
                selectedGender = "Masculino";
            } else if (i == R.id.radio_femenino) {
                selectedGender = "Femenino";
            }
        });

        radioGroup.check(R.id.radio_masculino);

        nameEditText.requestFocus();
        assert getDialog().getWindow() != null;
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
}
