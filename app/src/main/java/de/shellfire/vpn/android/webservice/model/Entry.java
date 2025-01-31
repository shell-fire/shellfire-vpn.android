package de.shellfire.vpn.android.webservice.model;

import org.jetbrains.annotations.NotNull;

public class Entry implements java.io.Serializable {
    private boolean boolEntry;
    private boolean starEntry;
    private boolean stringEntry;
    private boolean bool;
    private Star star;
    private String text;

    public Entry() {
    }

    public Entry(boolean boolEntry, boolean starEntry, boolean stringEntry, boolean bool, Star star, String text) {
        this.boolEntry = boolEntry;
        this.starEntry = starEntry;
        this.stringEntry = stringEntry;
        this.bool = bool;
        this.star = star;
        this.text = text;
    }

    public boolean isBoolEntry() {
        return boolEntry;
    }

    public void setBoolEntry(boolean boolEntry) {
        this.boolEntry = boolEntry;
    }

    public boolean isStarEntry() {
        return starEntry;
    }

    public void setStarEntry(boolean starEntry) {
        this.starEntry = starEntry;
    }

    public boolean isStringEntry() {
        return stringEntry;
    }

    public void setStringEntry(boolean stringEntry) {
        this.stringEntry = stringEntry;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public Star getStar() {
        return star;
    }

    public void setStar(Star star) {
        this.star = star;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @NotNull
    @Override
    public String toString() {
        if (boolEntry) {
            return Boolean.toString(bool);
        } else if (starEntry) {
            return star != null ? star.toString() : "N/A";
        } else if (stringEntry) {
            return text != null ? text : "N/A";
        } else {
            return "N/A";
        }
    }
}
