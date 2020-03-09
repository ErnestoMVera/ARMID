package mx.uabc.ahrs.events;

public class ControllerEvent {

    private int keyCode;

    public ControllerEvent(int keyCode) {
        this.keyCode = keyCode;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
