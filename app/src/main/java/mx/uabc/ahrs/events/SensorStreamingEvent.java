package mx.uabc.ahrs.events;

public class SensorStreamingEvent {

    public static final int STOP = 0;
    public static final int START = 1;

    private int action;

    public SensorStreamingEvent(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }
}
