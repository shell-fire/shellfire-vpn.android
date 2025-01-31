package de.shellfire.vpn.android;

import de.shellfire.vpn.android.webservice.model.Star;

public class VpnStar {

    private final int numStars;
    private String text;
    private int resId;

    public VpnStar(int i, String tr) {
        this.numStars = i;
        this.text = tr;
    }

    public VpnStar(int i, int resId) {
        this.numStars = i;
        this.resId = resId;
    }

    public VpnStar(Star star) {
        this.numStars = star.getNumStars();
        this.text = star.getText();
    }

    public int getNum() {
        return numStars;
    }

    public String getText() {
        return text;
    }

    public int getResId() {
        return resId;
    }
}
