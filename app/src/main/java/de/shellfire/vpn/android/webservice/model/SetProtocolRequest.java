package de.shellfire.vpn.android.webservice.model;

public class SetProtocolRequest {

    private int productId;
    private String proto;

    public SetProtocolRequest(int vpnProductId, String protocol) {
        this.productId = vpnProductId;
        this.proto = protocol;
    }

    public int getVpnProductId() {
        return productId;
    }

    public void setVpnProductId(int vpnProductId) {
        this.productId = vpnProductId;
    }

    public String getProtocol() {
        return proto;
    }

    public void setProtocol(String protocol) {
        this.proto = protocol;
    }

}
