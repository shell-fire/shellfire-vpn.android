package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

import de.shellfire.vpn.android.model.Alias;

public class AliasListContainer {

    @SerializedName("data")
    private Data data;


    public AliasListContainer() {
    }

    public AliasListContainer(Alias[] list) {
        this.data = new AliasListContainer.Data();
        this.data.setList(list);
    }

    public Alias[] getAliasList() {
        return data != null ? data.getAliasList() : new  Alias[0];
    }
    public String toString() { return data != null ? data.toString() : "null"; }


    private static class Data {
        @SerializedName("aliaslist")
        private Alias[] aliasList;

        public Alias[] getAliasList() {
            return aliasList;
        }

        public void setList(Alias[] aliasList) {
            this.aliasList = aliasList;
        }
    }
}
