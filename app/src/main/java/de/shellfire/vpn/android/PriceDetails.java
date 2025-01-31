package de.shellfire.vpn.android;

public class PriceDetails {
    private final double rawPrice; // Unformatierter Preis für Berechnungen
    private final String formattedPrice; // Formatierter Preis für Anzeigezwecke
    private final String currencyCode; // Währungscode für den Preis

    public PriceDetails(double rawPrice, String formattedPrice, String currencyCode) {
        this.rawPrice = rawPrice;
        this.formattedPrice = formattedPrice;
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public double getRawPrice() {
        return rawPrice;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    @Override
    public String toString() {
        return "PriceDetails{" +
                "rawPrice=" + rawPrice +
                ", formattedPrice='" + formattedPrice + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
