package de.shellfire.vpn.android;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author bettmenn
 */
public enum ProductType {
    PsyBnc(0),
    Eggdrop(1),
    Ts2Server(2),
    MumbleServer(3),
    Ts3Server(4),
    ShroudBnc(5),
    Pptp(6),
    OpenVpn(7),
    L2tp(8),
    Wireguard(10);

    final int id;

    ProductType(int id) {
        this.id = id;
    }

    public static ProductType getProductTypeById(int id) {
        ProductType type = null;
        for (ProductType productType : ProductType.values()) {
            if (productType.id == id) {
                type = productType;
                break;
            }
        }
        return type;
    }

}
