package mx.uabc.ahrs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.FirebaseApp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mx.uabc.ahrs.adapters.UsersAdapter;
import mx.uabc.ahrs.data.DatabaseManager;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.events.CreateUserEvent;
import mx.uabc.ahrs.fragments.CreateUserDialogFragment;

public class MainActivity extends AppCompatActivity {

    @Subscribe
    public void onCreateUserEvent(CreateUserEvent event) {

        User newUser = event.getUser();

        String trainingFilename = UUID.randomUUID().toString() + ".csv";
        String datasetFilename = UUID.randomUUID().toString() + ".csv";

        File trainingFile = new File(getFilesDir(), trainingFilename);

        if (!trainingFile.exists()) {
            try {
                boolean ignored = trainingFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File datasetFile = new File(getFilesDir(), datasetFilename);

        if (!datasetFile.exists()) {
            try {
                boolean ignored = datasetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        newUser.trainingFilename = trainingFilename;
        newUser.datasetFilename = datasetFilename;
        databaseManager.getUserDao().insert(newUser);
        fetchUsers();
    }

    private UsersAdapter adapter;
    private DatabaseManager databaseManager;

    @OnClick(R.id.fab)
    public void addUser() {
        showAddUserDialog();
    }

    private void showAddUserDialog() {
        FragmentManager fm = getSupportFragmentManager();
        CreateUserDialogFragment createUserDialogFragment = new CreateUserDialogFragment();
        createUserDialogFragment.show(fm, "fragment_create_user_dialog");
    }

    @BindView(R.id.listView)
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        databaseManager = DatabaseManager.getInstance(this);
        initUI();
        fetchUsers();
        FirebaseApp.initializeApp(this);
    }

    private void fetchUsers() {
        adapter.addItems(databaseManager.getUserDao().getAll());
    }

    private void initUI() {

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle("Sujetos");

        adapter = new UsersAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l)
                -> {
            User user = adapter.getItem(i);
            Intent intent = new Intent(MainActivity.this, SensorActivity.class);
            intent.putExtra(SensorActivity.USER_ID_PARAM, user.uid);
            startActivity(intent);
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
