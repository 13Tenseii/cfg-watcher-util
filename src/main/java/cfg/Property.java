package cfg;

public class Property {

    Property(Object value) {
        this.value = value.toString();
    }

    private String value;

    public String asText() {
        return value;
    }

    public Long asLong() {
        return Long.valueOf(value);
    }

    public Boolean asBoolean() {
        return Boolean.valueOf(value);
    }
}
