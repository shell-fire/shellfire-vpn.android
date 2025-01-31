package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

public class VpnAttributeList {
    @SerializedName("data")
    private Data data;

    public VpnAttributeList() {
    }

    public VpnAttributeList(VpnAttributeContainer[] containers) {
        this.data = new Data();
        this.data.setContainers(containers);
    }

    public VpnAttributeContainer[] getContainers() {
        return data != null ? data.getContainers() : new VpnAttributeContainer[0];
    }

    public void setContainers(VpnAttributeContainer[] containers) {
        if (this.data == null) {
            this.data = new Data();
        }
        this.data.setContainers(containers);
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (data != null && data.getContainers() != null) {
            for (VpnAttributeContainer container : data.getContainers()) {
                sb.append("Container: ").append(container.getContainerName()).append("\n");
                for (VpnAttributeElement element : container.getElements()) {
                    sb.append("  Element: ").append(element.getName()).append("\n");
                    sb.append("    Free: ").append(element.getFree()).append(" | ");
                    sb.append("Premium: ").append(element.getPremium()).append(" | ");
                    sb.append("PremiumPlus: ").append(element.getPp()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static class Data {
        @SerializedName("containers")
        private VpnAttributeContainer[] containers;

        public VpnAttributeContainer[] getContainers() {
            return containers;
        }

        public void setContainers(VpnAttributeContainer[] containers) {
            this.containers = containers;
        }
    }
}
