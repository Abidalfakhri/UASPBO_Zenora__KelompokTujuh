package com.zenora.controller;

import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class WhatIfController extends BaseModuleController implements javafx.fxml.Initializable {

    @Override public String moduleTitle() { return "What-If Simulator"; }

    @FXML private TextField targetField, monthlyField, rateField,
                            lumpSumField, inflationField, targetMonthsField;
    @FXML private TextArea resultArea;

    @Override
    public void initialize(java.net.URL u, java.util.ResourceBundle rb) {
        com.zenora.util.MoneyTextFormatter.attach(targetField);
        com.zenora.util.MoneyTextFormatter.attach(monthlyField);
        com.zenora.util.MoneyTextFormatter.attach(lumpSumField);
    }

    @FXML
    private void simulate() {
        InputValidator v = InputValidator.create();
        double target   = v.positiveDouble(targetField.getText(),   "Target dana");
        double monthly  = v.positiveDouble(monthlyField.getText(),  "Tabungan bulanan");
        double rate     = v.nonNegativeDouble(rateField.getText(),  "Return investasi (%)");

        // Opsional — default 0 kalau kosong
        double lumpSum   = parseOptional(lumpSumField.getText(),  0);
        double inflation = parseOptional(inflationField.getText(), 0);
        int    targetMonths = (int) Math.round(parseOptional(targetMonthsField.getText(), 0));

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }

        int monthsBase = monthsToReach(target, monthly, rate, lumpSum);
        double finalNominalBase = futureBalance(monthly, rate, lumpSum, monthsBase);

        StringBuilder sb = new StringBuilder();

        // ─── SKENARIO DASAR ─────────────────────────────────────────────
        sb.append("=== SKENARIO DASAR ===\n");
        sb.append(String.format("Target dana       : %s%n",  CurrencyFormatter.format(target)));
        sb.append(String.format("Top-up awal       : %s%n",  CurrencyFormatter.format(lumpSum)));
        sb.append(String.format("Tabungan/bulan    : %s%n",  CurrencyFormatter.format(monthly)));
        sb.append(String.format("Return investasi  : %.2f%% / tahun%n", rate));
        sb.append(String.format("Inflasi           : %.2f%% / tahun%n", inflation));
        sb.append(String.format("Estimasi tercapai : %s%n",  formatDuration(monthsBase)));
        sb.append(String.format("Saldo akhir       : %s%n%n", CurrencyFormatter.format(finalNominalBase)));

        // ─── DAMPAK INFLASI ─────────────────────────────────────────────
        if (inflation > 0 && monthsBase > 0) {
            double years = monthsBase / 12.0;
            double inflatedTarget = target * Math.pow(1 + inflation / 100.0, years);
            double realValue = finalNominalBase / Math.pow(1 + inflation / 100.0, years);
            sb.append("=== DAMPAK INFLASI ===\n");
            sb.append(String.format("Target setara nilai hari ini setelah %.1f thn : %s%n",
                    years, CurrencyFormatter.format(inflatedTarget)));
            sb.append(String.format("Daya beli %s nanti ≈ %s hari ini%n",
                    CurrencyFormatter.format(target),
                    CurrencyFormatter.format(realValue)));
            int monthsInfl = monthsToReach(inflatedTarget, monthly, rate, lumpSum);
            sb.append(String.format("Untuk mengejar target yg sudah ter-inflasi : %s%n%n",
                    formatDuration(monthsInfl)));
        }

        // ─── REVERSE: butuh nabung berapa untuk capai dalam X bulan? ────
        if (targetMonths > 0) {
            double effectiveTarget = Math.max(0, target - futureValueOfLumpSum(lumpSum, rate, targetMonths));
            double need = FinancialCalculator.requiredMonthlyContribution(effectiveTarget, rate, targetMonths);
            sb.append("=== TARGET WAKTU ===\n");
            sb.append(String.format("Untuk capai dalam %d bulan (%.1f tahun) :%n",
                    targetMonths, targetMonths / 12.0));
            sb.append(String.format("  butuh tabungan ≈ %s / bulan%n", CurrencyFormatter.format(need)));
            double diff = need - monthly;
            if (diff > 0)
                sb.append(String.format("  (%s lebih banyak dari rencana saat ini)%n%n",
                        CurrencyFormatter.format(diff)));
            else
                sb.append(String.format("  ✓ Rencana saat ini sudah cukup (selisih %s)%n%n",
                        CurrencyFormatter.format(Math.abs(diff))));
        }

        // ─── SKENARIO PERBANDINGAN ──────────────────────────────────────
        sb.append("=== SKENARIO PERBANDINGAN ===\n");
        appendScenario(sb, "TABUNGAN +25%",
                monthsToReach(target, monthly * 1.25, rate, lumpSum), monthsBase);
        appendScenario(sb, "TABUNGAN -25%",
                monthsToReach(target, monthly * 0.75, rate, lumpSum), monthsBase);
        appendScenario(sb, "RETURN +2%",
                monthsToReach(target, monthly, rate + 2, lumpSum), monthsBase);
        appendScenario(sb, "RETURN -2%",
                monthsToReach(target, monthly, Math.max(0, rate - 2), lumpSum), monthsBase);
        appendScenario(sb, "TARGET +25%",
                monthsToReach(target * 1.25, monthly, rate, lumpSum), monthsBase);
        appendScenario(sb, "TARGET -25%",
                monthsToReach(target * 0.75, monthly, rate, lumpSum), monthsBase);

        // Top-up tambahan: +10% target sebagai lump sum
        double extraLump = target * 0.10;
        appendScenario(sb, String.format("TOP-UP TAMBAHAN %s", CurrencyFormatter.format(extraLump)),
                monthsToReach(target, monthly, rate, lumpSum + extraLump), monthsBase);

        // ─── PROYEKSI WAKTU TETAP ───────────────────────────────────────
        sb.append("\n=== PROYEKSI SALDO BILA DIPERTAHANKAN ===\n");
        int[] horizons = {12, 36, 60, 120, 240};
        for (int m : horizons) {
            double bal = futureBalance(monthly, rate, lumpSum, m);
            sb.append(String.format("Setelah %3d bulan (%4.1f thn) : %s%n",
                    m, m / 12.0, CurrencyFormatter.format(bal)));
        }

        sb.append("\nGunakan kombinasi top-up, tabungan, dan return untuk menemukan rencana paling realistis.");
        resultArea.setText(sb.toString());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private double parseOptional(String raw, double fallback) {
        if (raw == null) return fallback;
        String s = raw.replaceAll("[^0-9.,-]", "").replace(",", ".").trim();
        if (s.isEmpty()) return fallback;
        try { return Double.parseDouble(s); } catch (Exception e) { return fallback; }
    }

    /** Simulasi bulanan: saldo = (saldo + setoran) * (1 + r/12). Berhenti saat ≥ target. */
    private int monthsToReach(double target, double monthly, double annualRatePct, double lumpSum) {
        if (target <= 0) return 0;
        if (lumpSum >= target) return 0;
        if (monthly <= 0 && annualRatePct <= 0) return Integer.MAX_VALUE / 2;
        double r = annualRatePct / 100.0 / 12.0;
        double bal = lumpSum;
        int max = 12 * 100; // 100 tahun
        for (int m = 1; m <= max; m++) {
            bal = (bal + monthly) * (1 + r);
            if (bal >= target) return m;
        }
        return max;
    }

    private double futureBalance(double monthly, double annualRatePct, double lumpSum, int months) {
        if (months <= 0) return lumpSum;
        double r = annualRatePct / 100.0 / 12.0;
        double bal = lumpSum;
        for (int i = 0; i < months; i++) bal = (bal + monthly) * (1 + r);
        return bal;
    }

    private double futureValueOfLumpSum(double lumpSum, double annualRatePct, int months) {
        double r = annualRatePct / 100.0 / 12.0;
        return lumpSum * Math.pow(1 + r, months);
    }

    private String formatDuration(int months) {
        if (months >= Integer.MAX_VALUE / 2) return "tidak tercapai";
        int years = months / 12, m = months % 12;
        if (years == 0) return months + " bulan";
        if (m == 0) return String.format("%d bulan (%d tahun)", months, years);
        return String.format("%d bulan (%d thn %d bln)", months, years, m);
    }

    private void appendScenario(StringBuilder sb, String label, int months, int base) {
        String effect;
        if (months >= Integer.MAX_VALUE / 2) effect = "tidak tercapai";
        else if (months == base) effect = "sama";
        else if (months < base)  effect = String.format("⏫ lebih cepat %d bulan", base - months);
        else                     effect = String.format("⏬ lebih lama %d bulan", months - base);
        sb.append(String.format("• %-32s : %s — %s%n",
                label, formatDuration(months), effect));
    }
}
