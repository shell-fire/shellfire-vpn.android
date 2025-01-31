package de.shellfire.vpn.android.webservice.model;

public class GetActivationStatusResponse {
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }
}
