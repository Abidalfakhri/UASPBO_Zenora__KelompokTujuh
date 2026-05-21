package com.zenora.controller;

import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class EmergencyController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Emergency Fund"; }

    @FXML private TextField expenseField, monthsField, capacityField;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private TextArea resultArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusChoice.setItems(FXCollections.observableArrayList(
                "Single (3 bulan)",
                "Menikah tanpa anak (6 bulan)",
                "Menikah + anak (12 bulan)",
                "Freelancer / Self-employed (12 bulan)"));
        statusChoice.getSelectionModel().selectFirst();
    }

    @FXML
    private void calculate() {
        InputValidator v = InputValidator.create();
        double expense  = v.positiveDouble(expenseField.getText(),  "Pengeluaran bulanan");
        double capacity = v.positiveDouble(capacityField.getText(), "Kapasitas menabung");

        if (v.hasErrors()) { resultArea.setText("⚠ " + v.errorMessage()); return; }

        int suggested = switch (statusChoice.getSelectionModel().getSelectedIndex()) {
            case 1 -> 6;
            case 2, 3 -> 12;
            default -> 3;
        };
        int months;
        if (monthsField.getText().trim().isEmpty()) {
            months = suggested;
        } else {
            InputValidator v2 = InputValidator.create();
            months = v2.positiveInt(monthsField.getText(), "Jumlah bulan");
            if (v2.hasErrors()) { resultArea.setText("⚠ " + v2.errorMessage()); return; }
        }

        double need          = FinancialCalculator.emergencyFundNeeded(expense, months);
        int monthsToBuild    = capacity > 0 ? (int) Math.ceil(need / capacity) : Integer.MAX_VALUE;

        StringBuilder sb = new StringBuilder();
        sb.append("=== EMERGENCY FUND ===\n\n");
        sb.append(String.format("Status                      : %s%n", statusChoice.getValue()));
        sb.append(String.format("Cakupan bulan               : %d bulan%n", months));
        sb.append(String.format("Pengeluaran bulanan         : %s%n", CurrencyFormatter.format(expense)));
        sb.append(String.format("%nDana darurat yang dibutuhkan: %s%n", CurrencyFormatter.format(need)));
        if (monthsToBuild < Integer.MAX_VALUE) {
            sb.append(String.format(
                    "%nDengan menabung %s/bulan,%nAnda butuh sekitar %d bulan (%.1f tahun).%n",
                    CurrencyFormatter.format(capacity), monthsToBuild, monthsToBuild / 12.0));
        }
        sb.append("\nTips:\n");
        sb.append("• Simpan di instrumen likuid: tabungan / reksa dana pasar uang.\n");
        sb.append("• Pisahkan dari rekening harian agar tidak terpakai.\n");
        sb.append("• Setelah terkumpul, lanjutkan ke goal lain (multi-goal planning).");
        resultArea.setText(sb.toString());
    }
}
