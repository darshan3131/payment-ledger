package com.darshan.payment_ledger.enums;

import lombok.Getter;

@Getter
public enum Currency {

    INR("₹", "Paise", 100),
    USD("$", "Cent", 100),
    EUR("€", "Cent", 100),
    GBP("£", "Penny", 100),
    JPY("¥", "Sen", 100),
    AED("AED", "Fils", 100),
    SGD("SGD", "Cent", 100),
    SAR("SAR", "Halala", 100),
    CAD("CAD", "Cent", 100),
    AUD("AUD", "Cent", 100);

    private final String symbol;
    private final String subUnit;
    private final int subUnitMultiplier;

    Currency(String symbol, String subUnit, int subUnitMultiplier) {
        this.symbol = symbol;
        this.subUnit = subUnit;
        this.subUnitMultiplier = subUnitMultiplier;
    }

    public String format(long amount) {
        double display = (double) amount / subUnitMultiplier;
        return String.format("%s%.2f", symbol, display);
    }
}