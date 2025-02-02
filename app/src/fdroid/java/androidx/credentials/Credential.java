package androidx.credentials;

/**
 * Stub for Credential to ensure compatibility in the F-Droid build.
 */
public class Credential {
    private final String type;
    private final Object data;

    public Credential(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
