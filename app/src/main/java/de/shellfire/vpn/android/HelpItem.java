package de.shellfire.vpn.android;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "help_items")
public class HelpItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String header;
    private String text;

    public HelpItem() {
        // Default constructor needed for Gson
    }

    public HelpItem(String header, String text) {
        this.header = header;
        this.text = text;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
