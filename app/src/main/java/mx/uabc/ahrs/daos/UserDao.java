package mx.uabc.ahrs.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import mx.uabc.ahrs.entities.User;

@Dao
public interface UserDao {

    @Query("SELECT * FROM user")
    List<User> getAll();

    @Query("SELECT * FROM user WHERE uid = (:userId) LIMIT 1")
    User loadById(int userId);

    @Insert
    void insert(User user);

    @Update
    public void updateUsers(User user);

    @Delete
    void delete(User user);
}
