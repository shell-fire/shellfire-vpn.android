package de.shellfire.vpn.android.webservice.model;

public class RegisterWithGoogleRequest {


    private String idToken;
    private int newsletter;

    public RegisterWithGoogleRequest(String idToken,  int subscribeToNewsletter) {
        this.idToken = idToken;
        this.newsletter = subscribeToNewsletter;
    }

    public int getSubscribeToNewsletter() {
        return newsletter;
    }

    public void setSubscribeToNewsletter(int subscribeToNewsletter) {
        this.newsletter = subscribeToNewsletter;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

}
