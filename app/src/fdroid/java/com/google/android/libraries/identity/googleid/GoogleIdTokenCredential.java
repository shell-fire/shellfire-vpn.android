package com.google.android.libraries.identity.googleid;

import androidx.credentials.CustomCredential;

public class GoogleIdTokenCredential extends CustomCredential {

    public static final String TYPE_GOOGLE_ID_TOKEN_CREDENTIAL = "STUB_GOOGLE_ID_TOKEN";

    private GoogleIdTokenCredential() {
        super(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL);
    }

    public static GoogleIdTokenCredential createFrom(Object data) {
        return new GoogleIdTokenCredential();
    }

    public String getIdToken() {
        return "stub-token";
    }
}
