package mx.uabc.ahrs.events;

import android.graphics.Bitmap;

import mx.uabc.ahrs.entities.User;

public class CreateUserEvent {

    private User user;
    private Bitmap sign;

    public CreateUserEvent(User user, Bitmap sign) {
        this.user = user;
        this.sign = sign;
    }

    public User getUser() {
        return user;
    }

    public Bitmap getSign() {
        return sign;
    }
}
