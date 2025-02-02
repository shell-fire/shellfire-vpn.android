package androidx.credentials;

/**
 * Stub for CustomCredential to allow compilation in the F-Droid build.
 */
public class CustomCredential extends Credential {
    private final String type;

    protected CustomCredential(String type) {
        super(type, null);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
