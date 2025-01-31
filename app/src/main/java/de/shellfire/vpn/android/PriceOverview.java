package de.shellfire.vpn.android;

import java.text.DecimalFormat;

public class PriceOverview {
    private PriceDetails yearlyPriceNormal;
    private PriceDetails yearlyPricePerMonthNormal;

    private PriceDetails yearlyPriceDiscounted;
    private PriceDetails yearlyPricePerMonthDiscounted;

    private PriceDetails monthlyPrice;

    // Getters
    public PriceDetails getYearlyPriceNormal() {
        return yearlyPriceNormal;
    }

    public PriceDetails getYearlyPricePerMonthNormal() {
        return yearlyPricePerMonthNormal;
    }

    public PriceDetails getYearlyPriceDiscounted() {
        return yearlyPriceDiscounted;
    }

    public PriceDetails getYearlyPricePerMonthDiscounted() {
        return yearlyPricePerMonthDiscounted;
    }

    public PriceDetails getMonthlyPrice() {
        return monthlyPrice;
    }

    // Setters
    public void setYearlyPriceNormal(PriceDetails yearlyPriceNormal) {
        this.yearlyPriceNormal = yearlyPriceNormal;
    }

    public void setYearlyPricePerMonthNormal(PriceDetails yearlyPricePerMonthNormal) {
        this.yearlyPricePerMonthNormal = yearlyPricePerMonthNormal;
    }

    public void setYearlyPriceDiscounted(PriceDetails yearlyPriceDiscounted) {
        this.yearlyPriceDiscounted = yearlyPriceDiscounted;
    }

    public void setYearlyPricePerMonthDiscounted(PriceDetails yearlyPricePerMonthDiscounted) {
        this.yearlyPricePerMonthDiscounted = yearlyPricePerMonthDiscounted;
    }

    public void setMonthlyPrice(PriceDetails monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    // Utility Methods
    public boolean hasDiscountForYearly() {
        return getYearlyPriceNormalValue() > 0 && getYearlyPriceDiscountedValue() > 0 &&
                getYearlyPriceDiscountedValue() < getYearlyPriceNormalValue();
    }

    public double getDiscountForYearly() {
        if (hasDiscountForYearly()) {
            double normalPrice = getYearlyPriceNormalValue();
            double discountedPrice = getYearlyPriceDiscountedValue();
            return ((normalPrice - discountedPrice) / normalPrice) * 100.0;
        }
        return 0.0;
    }

    // Helper Methods
    private double getYearlyPriceNormalValue() {
        return yearlyPriceNormal != null ? yearlyPriceNormal.getRawPrice() : 0.0;
    }

    private double getYearlyPriceDiscountedValue() {
        return yearlyPriceDiscounted != null ? yearlyPriceDiscounted.getRawPrice() : 0.0;
    }

    public String getFormattedDiscountForYearly() {
        if (hasDiscountForYearly()) {
            double discount = getDiscountForYearly();
            DecimalFormat decimalFormat = new DecimalFormat("0");
            return decimalFormat.format(discount) + "%";
        }
        return "";
    }


    // Optional: toString for debugging/logging
    @Override
    public String toString() {
        return "PriceOverview{" +
                "yearlyPriceNormal=" + yearlyPriceNormal +
                ", yearlyPricePerMonthNormal=" + yearlyPricePerMonthNormal +
                ", yearlyPriceDiscounted=" + yearlyPriceDiscounted +
                ", yearlyPricePerMonthDiscounted=" + yearlyPricePerMonthDiscounted +
                ", monthlyPrice=" + monthlyPrice +
                ", discountForYearly=" + getDiscountForYearly() +
                '}';
    }
}
