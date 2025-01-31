package de.shellfire.vpn.android.webservice.model;

public class SetServerToRequest {
    public final int productId;
    public final int serverId;
    public SetServerToRequest(int productId, int serverId) {
        this.productId = productId;
        this.serverId = serverId;
    }

}
