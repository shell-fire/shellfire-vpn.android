package com.google.android.libraries.identity.googleid;

import androidx.credentials.CredentialOption;

public class GetGoogleIdOption extends CredentialOption {

    private GetGoogleIdOption() {
        super("stub-google-id-option"); // Set a placeholder type
    }

    public static class Builder {
        public Builder setFilterByAuthorizedAccounts(boolean value) {
            return this;
        }

        public Builder setServerClientId(String clientId) {
            return this;
        }

        public Builder setAutoSelectEnabled(boolean value) {
            return this;
        }

        public Builder setNonce(String nonce) {
            return this;
        }

        public GetGoogleIdOption build() {
            return new GetGoogleIdOption();
        }
    }
}
