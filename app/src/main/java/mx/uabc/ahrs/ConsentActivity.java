package mx.uabc.ahrs;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;

public class ConsentActivity extends AppCompatActivity {

    public static final String USER_ID_PARAM = "userIdParam";

    private User user;

    @BindView(R.id.fecha)
    TextView fecha;
    @BindView(R.id.consentimiento)
    TextView consentimiento;
    @BindView(R.id.firma)
    ImageView firma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);

        ButterKnife.bind(this);

        int userId = getIntent().getIntExtra(USER_ID_PARAM, -1);
        user = DatabaseManager.getInstance(this).getUserDao().loadById(userId);

        if (userId == -1 || user == null) {
            Toast.makeText(this, "Hubo un problema", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle("Consentimiento");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initUI();
    }

    private void initUI() {

        String consentimientoText = getString(R.string.consentimiento);
        consentimientoText = "c" + consentimientoText.substring(1);
        String consentimientoCompleto = getString(R.string.consentimiento_template,
                user.name, consentimientoText);

        File signFile = new File(getFilesDir(), user.signFilename);

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = format.format(user.signingDate);
        fecha.setText(formattedDate);

        consentimiento.setText(consentimientoCompleto);
        Glide.with(this)
                .load(signFile)
                .into(firma);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
