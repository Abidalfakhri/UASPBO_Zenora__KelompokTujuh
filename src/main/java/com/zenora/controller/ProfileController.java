package com.zenora.controller;

import com.google.gson.JsonObject;
import com.zenora.app.AppSession;
import com.zenora.model.DataStore;
import com.zenora.model.UserProfile;
import com.zenora.service.ApiClient;
import com.zenora.service.StorageService;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ProfileController — diperbarui untuk menyimpan profil ke REST API backend.
 *
 * Perubahan:
 *   - save() → POST /api/profile (jika belum ada) atau PUT /api/profile/{id}
 *   - Data profil dari backend diload di DashboardController (saat sync)
 *   - Local DataStore tetap diupdate untuk kompatibilitas modul lain
 */
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
            // Update local DataStore
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

            // Build JSON body untuk API
            ProfileRequest req = new ProfileRequest(p);

            // Simpan ke backend di background thread
            Thread thread = new Thread(() -> {
                ApiClient.ApiResponse resp;
                String profileId = AppSession.getInstance().getProfileId();

                if (profileId != null && !profileId.isBlank()) {
                    // Update profil yang sudah ada
                    resp = ApiClient.put("/api/profile/" + profileId, req);
                } else {
                    // Buat profil baru
                    resp = ApiClient.post("/api/profile", req);
                    if (resp.isSuccess()) {
                        try {
                            JsonObject obj = ApiClient.parseObject(resp.body);
                            if (obj.has("id")) {
                                AppSession.getInstance().setProfileId(obj.get("id").getAsString());
                            }
                        } catch (Exception ignored) {}
                    }
                }

                final boolean ok = resp.isSuccess();
                final String msg = ok ? "Profil tersimpan ke database." : "Tersimpan lokal. Backend: " + resp.errorMessage();

                Platform.runLater(() -> {
                    new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, msg).showAndWait();
                    if (ok) {
                        // Setelah profil tersimpan, lanjut ke Dashboard.
                        SceneNavigator.navigateTo("/com/zenora/fxml/Dashboard.fxml");
                    }
                });
            });
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Input tidak valid: " + e.getMessage()).showAndWait();
        }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class ProfileRequest {
        String name;
        int age;
        double monthlyIncome;
        double monthlyExpense;
        int emergencyMonths;
        String householdStatus;
        double inflationPct;

        ProfileRequest(UserProfile p) {
            this.name            = p.getName();
            this.age             = p.getAge();
            this.monthlyIncome   = p.getMonthlyIncome();
            this.monthlyExpense  = p.getMonthlyExpense();
            this.emergencyMonths = p.getEmergencyMonths();
            this.householdStatus = p.getHouseholdStatus();
            this.inflationPct    = p.getInflationPct();
        }
    }
}
