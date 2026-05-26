package com.zenora.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


public final class MoneyTextFormatter {

    private static final DecimalFormatSymbols SYMS = new DecimalFormatSymbols(new Locale("id", "ID"));
    private static final DecimalFormat FMT = new DecimalFormat("#,###", SYMS);

    private MoneyTextFormatter() {}

    /** Pasang formatter pada sebuah TextField. */
    public static void attach(TextField field) {
        if (field == null) return;

        TextFormatter<String> tf = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;

           
            String digits = newText.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                change.setText("");
                change.setRange(0, change.getControlText().length());
                change.setCaretPosition(0);
                change.setAnchor(0);
                return change;
            }

            // Batasi panjang agar tidak overflow long
            if (digits.length() > 15) return null;

            String formatted = FMT.format(Long.parseLong(digits));
            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        });

        field.setTextFormatter(tf);

        // Format ulang teks awal (mis. dari DB) bila ada
        String initial = field.getText();
        if (initial != null && !initial.isBlank()) {
            String digits = initial.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                field.setText(FMT.format(Long.parseLong(digits)));
            }
        }
    }

    /** Parse teks ber-titik kembali menjadi angka. Kembalikan 0 bila kosong / invalid. */
    public static double parse(String raw) {
        if (raw == null) return 0;
        String digits = raw.replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || digits.equals("-")) return 0;
        try { return Double.parseDouble(digits); }
        catch (NumberFormatException e) { return 0; }
    }
}
