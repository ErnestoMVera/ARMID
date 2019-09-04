package mx.uabc.ahrs.models;

public class Setting {

    private String title;
    private String value;

    public Setting(String title, String value) {
        this.title = title;
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }
}
