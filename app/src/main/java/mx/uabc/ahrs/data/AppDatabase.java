package mx.uabc.ahrs.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import mx.uabc.ahrs.daos.UserDao;
import mx.uabc.ahrs.entities.User;

@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
