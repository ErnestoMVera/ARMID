package mx.uabc.ahrs.events;

import mx.uabc.ahrs.entities.User;

public class CreateUserEvent {

    private User user;

    public CreateUserEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
