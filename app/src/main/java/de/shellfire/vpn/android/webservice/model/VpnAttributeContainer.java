package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

public class VpnAttributeContainer implements java.io.Serializable {
    @SerializedName("containerName")
    private String containerName;

    @SerializedName("elements")
    private VpnAttributeElement[] elements;

    public VpnAttributeContainer() {
    }

    public VpnAttributeContainer(String containerName, VpnAttributeElement[] elements) {
        this.containerName = containerName;
        this.elements = elements;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public VpnAttributeElement[] getElements() {
        return elements;
    }

    public void setElements(VpnAttributeElement[] elements) {
        this.elements = elements;
    }
}
