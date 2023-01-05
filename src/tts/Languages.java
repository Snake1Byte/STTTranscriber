package tts;

public enum Languages {
    ENGLISH("en", "English"), GERMAN("de", "German"), ITALIAN("it", "Italian");


    private String code;
    private String name;

    private Languages(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
