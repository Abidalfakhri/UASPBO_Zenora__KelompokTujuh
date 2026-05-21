package com.zenora.controller;

import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MilestoneController extends BaseModuleController {

    @Override public String moduleTitle() { return "Milestone Roadmap"; }
    @FXML private TextField targetField, totalMonthsField, periodField, rateField;
    @FXML private TextArea resultArea;

    @FXML
    private void generate() {
        InputValidator v = InputValidator.create();
        double target    = v.positiveDouble(targetField.getText(),      "Target dana");
        int totalMonths  = v.positiveInt(totalMonthsField.getText(),    "Total jangka waktu (bulan)");
        int period       = v.positiveInt(periodField.getText(),         "Periode milestone (bulan)");
        double rate      = v.nonNegativeDouble(rateField.getText(),     "Return investasi (%)");

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }

        if (period > totalMonths) {
            new Alert(Alert.AlertType.WARNING,
                    "Periode milestone tidak boleh lebih besar dari total jangka waktu.").showAndWait();
            return;
        }

        double monthly    = FinancialCalculator.requiredMonthlyContribution(target, rate, totalMonths);
        int milestones    = (int) Math.ceil((double) totalMonths / period);
        double r          = rate / 100.0 / 12.0;

        StringBuilder sb = new StringBuilder();
        sb.append("=== MILESTONE PLAN ===\n\n");
        sb.append(String.format("Target total          : %s%n", CurrencyFormatter.format(target)));
        sb.append(String.format("Jangka waktu          : %d bulan (%.1f tahun)%n", totalMonths, totalMonths / 12.0));
        sb.append(String.format("Tabungan bulanan      : %s%n", CurrencyFormatter.format(monthly)));
        sb.append(String.format("Return investasi      : %.2f%% / tahun%n", rate));
        sb.append(String.format("Jumlah milestone      : %d checkpoint (setiap %d bulan)%n%n", milestones, period));

        for (int i = 1; i <= milestones; i++) {
            int monthsAtMs = Math.min(i * period, totalMonths);
            double balance;
            if (r == 0) {
                balance = monthly * monthsAtMs;
            } else {
                balance = monthly * ((Math.pow(1 + r, monthsAtMs) - 1) / r);
            }
            double pct = (balance / target) * 100;
            sb.append(String.format("Milestone %2d  Bulan %3d  :  %s  (%.1f%%)%n",
                    i, monthsAtMs, CurrencyFormatter.format(balance), pct));
        }

        sb.append("\n---\n");
        sb.append("Gunakan milestone ini sebagai checkpoint evaluasi.\n");
        sb.append("Jika aktual < milestone, review pengeluaran atau naikkan setoran.\n");
        sb.append("Catat setoran nyata via modul Contribution Log.");
        resultArea.setText(sb.toString());
    }
}
