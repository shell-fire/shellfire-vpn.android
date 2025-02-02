package androidx.credentials;

/**
 * Stub for CredentialOption to allow compilation in the F-Droid build.
 */
public abstract class CredentialOption {
    private final String type;

    protected CredentialOption(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
