package com.darshan.payment_ledger.util;

import com.darshan.payment_ledger.enums.Currency;

import java.util.Map;

/**
 * Static FX conversion utility for cross-currency transfers.
 *
 * All amounts in the system are stored in subunits (paise, cents, etc.).
 * Rates are major-unit rates (e.g., 1 USD = 84 INR).
 * To convert subunits: convertedSubunits = sourceSubunits × rate(src→dst)
 *
 * Example:
 *   $10.00 = 1000 cents (USD)
 *   rate(USD→INR) = 84
 *   result = 1000 × 84 = 84000 paise = ₹840.00  ✓
 *
 * Rates are approximate and hardcoded for demo purposes.
 * In production, replace with a live FX feed (Open Exchange Rates, ECB, etc.)
 */
public final class CurrencyConverter {

    private CurrencyConverter() {}

    // All rates as double: how many units of DESTINATION currency = 1 unit of SOURCE currency
    // Base: 1 unit of source major-currency → X units of destination major-currency
    private static final Map<String, Double> RATE_TO_INR = Map.of(
            "INR", 1.0,
            "USD", 84.0,
            "EUR", 91.0,
            "GBP", 107.0,
            "JPY", 0.56,
            "AED", 22.87,
            "SGD", 62.5,
            "SAR", 22.4,
            "CAD", 61.5,
            "AUD", 54.0
    );

    /**
     * Convert an amount in source currency subunits to destination currency subunits.
     * If source == destination, returns the amount unchanged.
     *
     * @param amount     amount in source currency subunits (e.g., cents, paise)
     * @param source     source Currency
     * @param destination destination Currency
     * @return amount in destination currency subunits
     */
    public static long convert(long amount, Currency source, Currency destination) {
        if (source == destination) return amount;

        Double srcToInr = RATE_TO_INR.get(source.name());
        Double dstToInr = RATE_TO_INR.get(destination.name());

        if (srcToInr == null || dstToInr == null) {
            throw new IllegalArgumentException(
                    "Unsupported currency pair: " + source + " → " + destination);
        }

        // Convert: src_subunits → src_major → INR_major → dst_major → dst_subunits
        // All currencies have subUnitMultiplier=100, so they cancel out:
        //   dst_subunits = src_subunits × (srcToInr / dstToInr)
        double rate = srcToInr / dstToInr;
        return Math.round(amount * rate);
    }

    /**
     * Return the FX rate as a human-readable string (for logging/display).
     * e.g. "1 USD = 84.00 INR"
     */
    public static String rateDescription(Currency source, Currency destination) {
        if (source == destination) return "1 " + source.name() + " = 1 " + destination.name();
        Double srcToInr = RATE_TO_INR.get(source.name());
        Double dstToInr = RATE_TO_INR.get(destination.name());
        if (srcToInr == null || dstToInr == null) return source + "→" + destination + " (unknown rate)";
        double rate = srcToInr / dstToInr;
        return String.format("1 %s = %.4f %s", source.name(), rate, destination.name());
    }
}
