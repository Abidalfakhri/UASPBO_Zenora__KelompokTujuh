package com.zenora.controller;

import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RetirementController extends BaseModuleController {

    @Override public String moduleTitle() { return "Retirement Planning"; }

    @FXML private TextField currentAgeField, retirementAgeField, yearsInRetirementField,
            monthlyNeedField, inflationField, returnPreField, returnPostField;
    @FXML private TextArea resultArea;

    @FXML
    private void calculate() {
        InputValidator v = InputValidator.create();
        int  currentAge       = v.positiveInt(currentAgeField.getText(),         "Usia saat ini");
        int  retirementAge    = v.positiveInt(retirementAgeField.getText(),       "Usia pensiun");
        int  yearsInRetirement= v.positiveInt(yearsInRetirementField.getText(),   "Lama pensiun (tahun)");
        double monthlyNeed    = v.positiveDouble(monthlyNeedField.getText(),      "Kebutuhan bulanan");
        double inflation      = v.nonNegativeDouble(inflationField.getText(),     "Inflasi (%)");
        double rPre           = v.nonNegativeDouble(returnPreField.getText(),     "Return pra-pensiun (%)");
        double rPost          = v.nonNegativeDouble(returnPostField.getText(),    "Return pasca-pensiun (%)");

        if (v.hasErrors()) { resultArea.setText("⚠ " + v.errorMessage()); return; }

        if (retirementAge <= currentAge) {
            resultArea.setText("⚠ Usia pensiun harus lebih besar dari usia saat ini.");
            return;
        }

        int yearsToRetire = retirementAge - currentAge;

        double fundNeeded       = FinancialCalculator.retirementFundNeeded(
                monthlyNeed, inflation, yearsToRetire, yearsInRetirement, rPost);
        double monthlyAtRetire  = FinancialCalculator.inflateAmount(monthlyNeed, inflation, yearsToRetire);
        double monthlyContrib   = FinancialCalculator.requiredMonthlyContribution(
                fundNeeded, rPre, yearsToRetire * 12);

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTIMASI DANA PENSIUN ===\n\n");
        sb.append(String.format("Tahun menuju pensiun            : %d tahun%n", yearsToRetire));
        sb.append(String.format("Kebutuhan bulanan (kini)        : %s%n",
                CurrencyFormatter.format(monthlyNeed)));
        sb.append(String.format("Kebutuhan bulanan saat pensiun  : %s%n",
                CurrencyFormatter.format(monthlyAtRetire)));
        sb.append(String.format("%nTotal dana pensiun dibutuhkan   : %s%n",
                CurrencyFormatter.format(fundNeeded)));
        sb.append(String.format("%n→ Anda perlu menabung/investasi sekitar:%n   %s per bulan%n"
                + "   selama %d tahun (%d bulan)%n",
                CurrencyFormatter.format(monthlyContrib),
                yearsToRetire, yearsToRetire * 12));
        sb.append(String.format(
                "%nAsumsi: return pra-pensiun %.1f%%/thn, pasca-pensiun %.1f%%/thn, inflasi %.1f%%/thn.",
                rPre, rPost, inflation));
        resultArea.setText(sb.toString());
    }
}
