package mx.uabc.ahrs.data;

import android.content.Context;

import androidx.room.Room;

import mx.uabc.ahrs.daos.UserDao;

public class DatabaseManager {

    private static volatile DatabaseManager instance;
    private AppDatabase db;

    public static DatabaseManager getInstance(Context context) {

        if (instance == null)
            instance = new DatabaseManager(context);

        return instance;
    }

    private DatabaseManager(Context context) {
        db = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "database-name").allowMainThreadQueries().build();
    }

    public UserDao getUserDao() {
        return db.userDao();
    }

}
