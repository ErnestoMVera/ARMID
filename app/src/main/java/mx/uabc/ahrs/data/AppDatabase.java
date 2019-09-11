package mx.uabc.ahrs.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import mx.uabc.ahrs.daos.UserDao;
import mx.uabc.ahrs.entities.User;
import mx.uabc.ahrs.helpers.Converters;

@Database(entities = {User.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
