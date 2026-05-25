package com.zenora.controller;

import com.google.gson.JsonObject;
import com.zenora.model.Category;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.ApiClient;
import com.zenora.service.FinancialCalculator;
import com.zenora.service.RecommendationEngine;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * GoalController — diperbarui untuk menyimpan goal ke REST API backend.
 *
 * Perubahan:
 *   - saveToMulti() → POST /api/goals setelah konfirmasi
 *   - ID dari backend disimpan ke Goal lokal agar bisa di-update/delete nanti
 *   - Jika backend tidak tersedia, fallback ke DataStore lokal
 */
public class GoalController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Goal Planning"; }

    @FXML private TextField nameField, targetField, monthsField, rateField, capacityField, storageLocationField;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<String> storageTypeChoice;
    @FXML private DatePicker targetDatePicker;
    @FXML private TextArea resultArea;
    @FXML private Button saveButton;

    private Goal pendingGoal;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (categoryChoice != null) {
            categoryChoice.setItems(FXCollections.observableArrayList(Category.values()));
            categoryChoice.setValue(Category.UMUM);
        }
        if (storageTypeChoice != null) {
            storageTypeChoice.setItems(FXCollections.observableArrayList(
                    "Bank", "E-Wallet", "Investasi", "Tunai", "Lainnya"));
            storageTypeChoice.setValue("Bank");
        }
        if (saveButton != null) saveButton.setDisable(true);

        com.zenora.util.MoneyTextFormatter.attach(targetField);
        com.zenora.util.MoneyTextFormatter.attach(capacityField);

        double cap = DataStore.getInstance().getProfile().effectiveCapacity();
        if (cap > 0 && capacityField != null) capacityField.setText(String.valueOf((long) cap));
    }

    @FXML
    private void calculate() {
        InputValidator v = InputValidator.create();
        String  name     = nameField.getText().trim();
        double  target   = v.positiveDouble(targetField.getText(),   "Target dana");
        int     months   = v.positiveInt(monthsField.getText(),      "Jangka waktu (bulan)");
        double  rate     = v.nonNegativeDouble(rateField.getText(),   "Return tahunan (%)");
        double  capacity = v.positiveDouble(capacityField.getText(),  "Kapasitas menabung");

        if (v.hasErrors()) {
            alert(Alert.AlertType.WARNING, "Input tidak valid", v.errorMessage());
            if (saveButton != null) saveButton.setDisable(true);

        com.zenora.util.MoneyTextFormatter.attach(targetField);
        com.zenora.util.MoneyTextFormatter.attach(capacityField);
            return;
        }

        DataStore.getInstance().getProfile().setMonthlyCapacityOverride(capacity);

        Goal g = new Goal(name.isEmpty() ? "Goal" : name, target, months, rate, 2);
        if (categoryChoice != null && categoryChoice.getValue() != null)
            g.setCategory(categoryChoice.getValue());
        if (storageTypeChoice != null && storageTypeChoice.getValue() != null)
            g.setStorageType(storageTypeChoice.getValue());
        if (storageLocationField != null)
            g.setStorageLocation(storageLocationField.getText().trim());

        if (targetDatePicker != null && targetDatePicker.getValue() != null)
            g.setTargetDate(targetDatePicker.getValue());
        else
            g.setTargetDate(LocalDate.now().plusMonths(months));

        pendingGoal = g;

        double monthly          = FinancialCalculator.requiredMonthlyContribution(target, rate, months);
        g.setMonthlySaving(monthly);
        List<Double> projection = FinancialCalculator.projectGrowth(monthly, rate, months);
        RecommendationEngine.Recommendation rec = RecommendationEngine.analyze(g, capacity);

        StringBuilder sb = new StringBuilder();
        sb.append("=== HASIL PERHITUNGAN ===\n");
        sb.append(String.format("Goal             : %s (%s)%n", g.getName(), g.getCategory().getLabel()));
        sb.append(String.format("Target dana      : %s%n", CurrencyFormatter.format(target)));
        sb.append(String.format("Jangka waktu     : %d bulan (%.1f tahun)%n", months, months / 12.0));
        sb.append(String.format("Target tercapai  : %s%n", g.getTargetDate()));
        sb.append(String.format("Tabungan bulanan : %s%n", CurrencyFormatter.format(monthly)));

        if (!rec.warnings.isEmpty()) {
            sb.append("\n=== PERHATIAN ===\n");
            for (String w : rec.warnings) sb.append("⚠ ").append(w).append("\n");
        }

        sb.append("\n=== FEASIBILITY CHECK ===\n");
        sb.append(rec.status).append("\n\n");
        sb.append("Saran:\n");
        for (String s : rec.suggestions) sb.append("• ").append(s).append("\n");

        sb.append("\n=== SIMULASI PERTUMBUHAN ===\n");
        int step = Math.max(1, months / 10);
        for (int i = step - 1; i < projection.size(); i += step)
            sb.append(String.format("Bulan %3d : %s%n", i + 1, CurrencyFormatter.format(projection.get(i))));
        int last = projection.size() - 1;
        if (last >= 0 && (last % step) != (step - 1))
            sb.append(String.format("Bulan %3d : %s%n", last + 1, CurrencyFormatter.format(projection.get(last))));

        sb.append("\nTekan \"Simpan\" untuk menyimpan goal ini ke database.");
        resultArea.setText(sb.toString());
        if (saveButton != null) saveButton.setDisable(false);
    }

    @FXML
    private void saveToMulti() {
        if (pendingGoal == null) {
            alert(Alert.AlertType.WARNING, "Belum dihitung", "Hitung terlebih dahulu sebelum menyimpan.");
            return;
        }

        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Simpan goal \"" + pendingGoal.getName() + "\" ke database?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.YES) return;

        if (saveButton != null) saveButton.setDisable(true);

        com.zenora.util.MoneyTextFormatter.attach(targetField);
        com.zenora.util.MoneyTextFormatter.attach(capacityField);

        final Goal toSave = pendingGoal;
        pendingGoal = null;

        // POST ke backend di background thread
        Thread thread = new Thread(() -> {
            GoalRequest req = new GoalRequest(toSave);
            ApiClient.ApiResponse resp = ApiClient.post("/api/goals", req);

            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    // Ambil ID yang digenerate backend dan simpan ke Goal lokal
                    try {
                        JsonObject obj = ApiClient.parseObject(resp.body);
                        if (obj.has("id")) toSave.setId(obj.get("id").getAsString());
                    } catch (Exception ignored) {}

                    DataStore.getInstance().getGoals().add(toSave);
                    alert(Alert.AlertType.INFORMATION, "Goal Tersimpan",
                            "Goal '" + toSave.getName() + "' berhasil disimpan ke database.\n"
                            + "Gunakan modul Contribution Log untuk mencatat setoran.");
                } else {
                    // Fallback: simpan ke DataStore lokal saja
                    DataStore.getInstance().getGoals().add(toSave);
                    alert(Alert.AlertType.WARNING, "Tersimpan Lokal",
                            "Goal disimpan lokal. Backend: " + resp.errorMessage());
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void alert(Alert.AlertType type, String header, String content) {
        Alert a = new Alert(type);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class GoalRequest {
        String name;
        double targetAmount;
        int months;
        double interestRate;
        int priority;
        String category;
        String storageType;
        String storageLocation;

        GoalRequest(Goal g) {
            this.name            = g.getName();
            this.targetAmount    = g.getTargetAmount();
            this.months          = g.getMonths();
            this.interestRate    = g.getInterestRate();
            this.priority        = g.getPriority();
            this.category        = g.getCategory() != null ? g.getCategory().name() : "UMUM";
            this.storageType     = g.getStorageType();
            this.storageLocation = g.getStorageLocation();
        }
    }
}
