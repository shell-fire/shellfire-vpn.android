package de.shellfire.vpn.android.webservice.model;

public class LoginResponse {

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String toString() { return token; }
}
