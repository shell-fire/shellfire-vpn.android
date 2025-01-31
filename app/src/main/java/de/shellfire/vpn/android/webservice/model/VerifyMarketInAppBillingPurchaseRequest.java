package de.shellfire.vpn.android.webservice.model;

public class VerifyMarketInAppBillingPurchaseRequest {
    private String signedData;
    private String signature;

    public void setSignedData(String signedData) {
        this.signedData = signedData;

    }

    public void setSignature(String signature) {
        this.signature = signature;

    }
}
