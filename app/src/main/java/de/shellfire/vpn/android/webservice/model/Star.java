package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

public class Star implements java.io.Serializable {
    @SerializedName("numStars")
    private int numStars;

    @SerializedName("text")
    private String text;

    public Star() {
    }

    public Star(int numStars, String text) {
        this.numStars = numStars;
        this.text = text;
    }

    public int getNumStars() {
        return numStars;
    }

    public void setNumStars(int numStars) {
        this.numStars = numStars;
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
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < numStars) {
                stars.append("★"); // filled star
            } else {
                stars.append("☆"); // unfilled star
            }
        }
        return stars + " (" + text + ")";
    }
}
