package de.shellfire.vpn.android.webservice.model;

import java.io.Serializable;

public class WsFile  implements Serializable {
    private static final long serialVersionUID = 1L; // Adding a version ID for the class

    private String name;
    private String content;

    public WsFile(String name, String content) {
        this.name = name;
        this.content = content;
    }

    /**
     * Gets the name value for this WsFile.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name value for this WsFile.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the content value for this WsFile.
     *
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content value for this WsFile.
     *
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }

}
