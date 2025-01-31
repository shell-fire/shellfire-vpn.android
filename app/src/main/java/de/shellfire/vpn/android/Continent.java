package de.shellfire.vpn.android;

/**
 * Created by Alina on 07.02.2018.
 */

public enum Continent {
    AFRICA("AFRICA"),
    ANTARCTICA("ANTARCTICA"),
    ASIA("ASIA"),
    AUSTRALIA("AUSTRALIA"),
    EUROPE("EUROPE"),
    NORTH_AMERICA("NORTH_AMERICA"),
    SOUTH_AMERICA("SOUTH_AMERICA"),

    OCEANIA("OCEANIA");

    private final String name;

    Continent(String name) {
        this.name = name;
    }

    public String continentName() {
        return name;
    }
}
