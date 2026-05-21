package com.zenora.util;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private static final NumberFormat IDR = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    public static String format(double amount) {
        return IDR.format(amount);
    }
}
