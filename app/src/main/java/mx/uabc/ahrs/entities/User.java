package mx.uabc.ahrs.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "age")
    public int age;

    @ColumnInfo(name = "gender")
    public String gender;

    @ColumnInfo(name = "training_filename")
    public String trainingFilename;

    @ColumnInfo(name = "dataset_filename")
    public String datasetFilename;

    public User(String name, int age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }
}
