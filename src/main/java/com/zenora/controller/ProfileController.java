package com.zenora.controller;

import com.zenora.model.DataStore;
import com.zenora.model.UserProfile;
import com.zenora.service.StorageService;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Profile"; }
    @FXML private TextField nameField, ageField, incomeField, expenseField,
            capacityOverrideField, inflationField;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private ChoiceBox<Integer> emergencyMonthsChoice;
    @FXML private Label derivedCapacityLabel, derivedRateLabel, derivedEfLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusChoice.setItems(FXCollections.observableArrayList(
                "Single", "Menikah tanpa anak", "Menikah + anak", "Freelancer / Self-employed"));
        emergencyMonthsChoice.setItems(FXCollections.observableArrayList(3, 6, 9, 12));

        UserProfile p = DataStore.getInstance().getProfile();
        nameField.setText(p.getName());
        ageField.setText(String.valueOf(p.getAge()));
        incomeField.setText(p.getMonthlyIncome() == 0 ? "" : String.valueOf((long) p.getMonthlyIncome()));
        expenseField.setText(p.getMonthlyExpense() == 0 ? "" : String.valueOf((long) p.getMonthlyExpense()));
        capacityOverrideField.setText(p.getMonthlyCapacityOverride() == 0 ? "" : String.valueOf((long) p.getMonthlyCapacityOverride()));
        inflationField.setText(String.valueOf(p.getInflationPct()));
        statusChoice.setValue(p.getHouseholdStatus());
        emergencyMonthsChoice.setValue(p.getEmergencyMonths());

        recompute();
        nameField.textProperty().addListener((o,a,b) -> recompute());
        incomeField.textProperty().addListener((o,a,b) -> recompute());
        expenseField.textProperty().addListener((o,a,b) -> recompute());
        capacityOverrideField.textProperty().addListener((o,a,b) -> recompute());
        emergencyMonthsChoice.valueProperty().addListener((o,a,b) -> recompute());
    }

    private void recompute() {
        double income = parse(incomeField.getText());
        double expense = parse(expenseField.getText());
        double override_ = parse(capacityOverrideField.getText());
        double capacity = override_ > 0 ? override_ : Math.max(0, income - expense);
        derivedCapacityLabel.setText(CurrencyFormatter.format(capacity) + " / bulan");
        double rate = income <= 0 ? 0 : Math.max(0, (income - expense) / income) * 100;
        derivedRateLabel.setText(String.format("%.1f%% dari pendapatan", rate));
        Integer m = emergencyMonthsChoice.getValue();
        if (m == null) m = 6;
        derivedEfLabel.setText(CurrencyFormatter.format(expense * m));
    }

    private double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(",", "")); }
        catch (Exception e) { return 0; }
    }

    @FXML
    private void save() {
        try {
            UserProfile p = DataStore.getInstance().getProfile();
            p.setName(nameField.getText().trim());
            p.setAge(Integer.parseInt(ageField.getText().trim().isEmpty() ? "0" : ageField.getText().trim()));
            p.setMonthlyIncome(parse(incomeField.getText()));
            p.setMonthlyExpense(parse(expenseField.getText()));
            p.setMonthlyCapacityOverride(parse(capacityOverrideField.getText()));
            p.setInflationPct(parse(inflationField.getText()));
            p.setHouseholdStatus(statusChoice.getValue());
            p.setEmergencyMonths(emergencyMonthsChoice.getValue() == null ? 6 : emergencyMonthsChoice.getValue());
            StorageService.save();
            new Alert(Alert.AlertType.INFORMATION, "Profil tersimpan permanen di " + StorageService.dataFile()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Input tidak valid: " + e.getMessage()).showAndWait();
        }
    }

}
