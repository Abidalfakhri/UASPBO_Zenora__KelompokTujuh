package com.zenora.controller;

import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class WhatIfController extends BaseModuleController {

    @Override public String moduleTitle() { return "What-If Simulator"; }
    @FXML private TextField targetField, monthlyField, rateField;
    @FXML private TextArea resultArea;

    @FXML
    private void simulate() {
        InputValidator v = InputValidator.create();
        double target  = v.positiveDouble(targetField.getText(),     "Target dana");
        double monthly = v.positiveDouble(monthlyField.getText(),    "Tabungan bulanan");
        double rate    = v.nonNegativeDouble(rateField.getText(),    "Return investasi (%)");

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }

        int monthsBase = FinancialCalculator.monthsToReachTarget(target, monthly, rate);

        StringBuilder sb = new StringBuilder();
        sb.append("=== SKENARIO DASAR ===\n");
        sb.append(String.format("Target           : %s%n",   CurrencyFormatter.format(target)));
        sb.append(String.format("Tabungan/bulan   : %s%n",   CurrencyFormatter.format(monthly)));
        sb.append(String.format("Return           : %.2f%% / tahun%n", rate));
        sb.append(String.format("Estimasi tercapai: %d bulan (%.1f tahun)%n%n", monthsBase, monthsBase / 12.0));

        appendScenario(sb, "TABUNGAN +25%",
                FinancialCalculator.monthsToReachTarget(target, monthly * 1.25, rate),
                monthsBase, true);
        appendScenario(sb, "TABUNGAN -25%",
                FinancialCalculator.monthsToReachTarget(target, monthly * 0.75, rate),
                monthsBase, false);
        appendScenario(sb, "RETURN +2%",
                FinancialCalculator.monthsToReachTarget(target, monthly, rate + 2),
                monthsBase, true);
        appendScenario(sb, "RETURN -2%",
                FinancialCalculator.monthsToReachTarget(target, monthly, Math.max(0, rate - 2)),
                monthsBase, false);
        appendScenario(sb, "TARGET +25%",
                FinancialCalculator.monthsToReachTarget(target * 1.25, monthly, rate),
                monthsBase, false);
        appendScenario(sb, "TARGET -25%",
                FinancialCalculator.monthsToReachTarget(target * 0.75, monthly, rate),
                monthsBase, true);

        sb.append("\nGunakan simulasi ini untuk memilih kombinasi yang paling realistis.");
        resultArea.setText(sb.toString());
    }

    private void appendScenario(StringBuilder sb, String label, int months, int base, boolean faster) {
        int diff = Math.abs(months - base);
        String effect = faster
                ? String.format("lebih cepat %d bulan", diff)
                : String.format("lebih lama %d bulan",  diff);
        if (months == base) effect = "sama";
        sb.append(String.format("=== JIKA %s ===%n", label));
        sb.append(String.format("Tercapai dalam %d bulan (%.1f tahun) — %s%n%n",
                months, months / 12.0, effect));
    }
}
