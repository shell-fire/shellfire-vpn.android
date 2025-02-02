package androidx.credentials;

/**
 * Stub for GetCredentialResponse to allow compilation in the F-Droid build.
 */
public class GetCredentialResponse {
    private final Credential credential;

    public GetCredentialResponse(Credential credential) {
        this.credential = credential;
    }

    public Credential getCredential() {
        return credential;
    }
}
