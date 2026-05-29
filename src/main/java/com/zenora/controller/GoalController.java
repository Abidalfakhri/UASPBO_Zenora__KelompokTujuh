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
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class GoalController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Goal Planning"; }

    @FXML private TextField nameField, targetField, monthsField, rateField, capacityField, storageLocationField;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private ChoiceBox<String> storageTypeChoice;
    @FXML private DatePicker targetDatePicker;
    @FXML private TextArea resultArea;
    @FXML private Button saveButton;
    @FXML private Label editingLabel;

    // ── Daftar goal tersimpan (edit/delete) ───────────────────────────────
    @FXML private TableView<Goal> goalsTable;
    @FXML private TableColumn<Goal, String> colName;
    @FXML private TableColumn<Goal, String> colCategory;
    @FXML private TableColumn<Goal, Number> colTarget;
    @FXML private TableColumn<Goal, Number> colMonths;
    @FXML private TableColumn<Goal, Number> colMonthly;

    private Goal pendingGoal;
    /** Jika != null, mode EDIT pada goal ini (PUT, bukan POST). */
    private Goal editingGoal;

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
        if (editingLabel != null) editingLabel.setVisible(false);

        com.zenora.util.MoneyTextFormatter.attach(targetField);
        com.zenora.util.MoneyTextFormatter.attach(capacityField);

        double cap = DataStore.getInstance().getProfile().effectiveCapacity();
        if (cap > 0 && capacityField != null) capacityField.setText(String.valueOf((long) cap));

        // ── Goals table ───────────────────────────────────────────────────
        if (goalsTable != null) {
            if (colName     != null) colName.setCellValueFactory(c -> c.getValue().nameProperty());
            if (colCategory != null) colCategory.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getCategory().getLabel()));
            if (colTarget   != null) { colTarget.setCellValueFactory(c -> c.getValue().targetAmountProperty()); formatCurrency(colTarget); }
            if (colMonths   != null) colMonths.setCellValueFactory(c -> c.getValue().monthsProperty());
            if (colMonthly  != null) { colMonthly.setCellValueFactory(c -> c.getValue().monthlySavingProperty()); formatCurrency(colMonthly); }
            goalsTable.setItems(DataStore.getInstance().getGoals());
        }
    }

    private void formatCurrency(TableColumn<Goal, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
    }

    // ── Calculate (hitung saja, belum simpan) ─────────────────────────────
    @FXML
    private void calculate() {
        InputValidator v = InputValidator.create();
        String  name     = nameField.getText().trim();
        double  target   = v.positiveDouble(targetField.getText(),    "Target dana");
        int     months   = v.positiveInt(monthsField.getText(),       "Jangka waktu (bulan)");
        double  rate     = v.nonNegativeDouble(rateField.getText(),   "Return tahunan (%)");
        double  capacity = v.positiveDouble(capacityField.getText(),  "Kapasitas menabung");

        if (v.hasErrors()) {
            alert(Alert.AlertType.WARNING, "Input tidak valid", v.errorMessage());
            if (saveButton != null) saveButton.setDisable(true);
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

        // Jika sedang edit, pertahankan ID lama agar bisa PUT.
        if (editingGoal != null) g.setId(editingGoal.getId());

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

        sb.append(editingGoal != null
                ? "\nTekan \"Simpan\" untuk memperbarui goal ini di database."
                : "\nTekan \"Simpan\" untuk menyimpan goal ini ke database.");
        resultArea.setText(sb.toString());
        if (saveButton != null) saveButton.setDisable(false);
    }

    // ── Save (POST kalau baru, PUT kalau edit) ────────────────────────────
    @FXML
    private void saveToMulti() {
        if (pendingGoal == null) {
            alert(Alert.AlertType.WARNING, "Belum dihitung", "Hitung terlebih dahulu sebelum menyimpan.");
            return;
        }

        boolean isEdit = editingGoal != null;
        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                (isEdit ? "Perbarui goal \"" : "Simpan goal \"")
                        + pendingGoal.getName() + "\" "
                        + (isEdit ? "di" : "ke") + " database?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.YES) return;

        if (saveButton != null) saveButton.setDisable(true);

        final Goal toSave = pendingGoal;
        final Goal previousEditingRef = editingGoal;
        pendingGoal = null;
        editingGoal = null;

        Thread thread = new Thread(() -> {
            GoalRequest req = new GoalRequest(toSave);
            ApiClient.ApiResponse resp = isEdit
                    ? ApiClient.put("/api/goals/" + toSave.getId(), req)
                    : ApiClient.post("/api/goals", req);

            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    if (!isEdit) {
                        try {
                            JsonObject obj = ApiClient.parseObject(resp.body);
                            if (obj.has("id")) toSave.setId(obj.get("id").getAsString());
                        } catch (Exception ignored) {}
                        DataStore.getInstance().getGoals().add(toSave);
                    } else {
                        replaceInStore(previousEditingRef, toSave);
                    }
                    clearForm();
                    alert(Alert.AlertType.INFORMATION,
                            isEdit ? "Goal Diperbarui" : "Goal Tersimpan",
                            (isEdit ? "Goal '" : "Goal '") + toSave.getName()
                                    + (isEdit ? "' berhasil diperbarui." : "' berhasil disimpan ke database."));
                } else {
                    // Fallback lokal
                    if (!isEdit) DataStore.getInstance().getGoals().add(toSave);
                    else        replaceInStore(previousEditingRef, toSave);
                    clearForm();
                    alert(Alert.AlertType.WARNING, "Tersimpan Lokal",
                            "Perubahan disimpan lokal. Backend: " + resp.errorMessage());
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void replaceInStore(Goal oldRef, Goal newRef) {
        var list = DataStore.getInstance().getGoals();
        int idx = list.indexOf(oldRef);
        if (idx >= 0) list.set(idx, newRef);
        else list.add(newRef);
    }

    // ── Edit selected ─────────────────────────────────────────────────────
    @FXML
    private void editSelected() {
        Goal sel = goalsTable == null ? null : goalsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.INFORMATION, "Pilih goal",
                    "Pilih dulu baris goal yang ingin diedit dari tabel.");
            return;
        }
        editingGoal = sel;

        nameField.setText(sel.getName());
        targetField.setText(String.valueOf((long) sel.getTargetAmount()));
        monthsField.setText(String.valueOf(sel.getMonths()));
        // Format rapi: 4.8 -> "4.8", 5.0 -> "5", 12.5 -> "12.5". Hindari "48.0" dsb.
        double _rate = sel.getInterestRate();
        // Pengaman tambahan jika ada data korup yang belum ter-koreksi (mis. dimuat lewat jalur lain)
        while (_rate > 100) _rate /= 10.0;
        rateField.setText(_rate == Math.floor(_rate)
                ? String.valueOf((long) _rate)
                : java.math.BigDecimal.valueOf(_rate)
                        .setScale(4, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString());
        if (categoryChoice != null && sel.getCategory() != null)
            categoryChoice.setValue(sel.getCategory());
        if (storageTypeChoice != null && sel.getStorageType() != null)
            storageTypeChoice.setValue(sel.getStorageType());
        if (storageLocationField != null)
            storageLocationField.setText(sel.getStorageLocation());
        if (targetDatePicker != null)
            targetDatePicker.setValue(sel.getTargetDate());

        if (editingLabel != null) {
            editingLabel.setText("✎  Mode Edit: " + sel.getName() + "  — tekan Hitung lalu Simpan untuk update");
            editingLabel.setVisible(true);
        }
        if (saveButton != null) saveButton.setDisable(true);
        resultArea.setText("Mode edit aktif. Ubah field yang perlu, lalu klik 'Hitung' diikuti 'Simpan'.");
    }

    @FXML
    private void cancelEdit() {
        editingGoal = null;
        pendingGoal = null;
        clearForm();
        if (saveButton != null) saveButton.setDisable(true);
        resultArea.clear();
    }

    // ── Delete selected ───────────────────────────────────────────────────
    @FXML
    private void deleteSelected() {
        Goal sel = goalsTable == null ? null : goalsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.INFORMATION, "Pilih goal",
                    "Pilih dulu baris goal yang ingin dihapus dari tabel.");
            return;
        }
        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus goal \"" + sel.getName() + "\" dari database?\n"
                        + "Tindakan ini tidak dapat dibatalkan.",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.YES) return;

        final Goal toRemove = sel;
        // Optimistically hapus dari UI, lalu sinkron ke backend
        DataStore.getInstance().getGoals().remove(toRemove);
        if (editingGoal == toRemove) cancelEdit();

        Thread t = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.delete("/api/goals/" + toRemove.getId());
            Platform.runLater(() -> {
                if (!resp.isSuccess()) {
                    alert(Alert.AlertType.WARNING, "Hapus Lokal Saja",
                            "Goal dihapus dari aplikasi, tapi backend gagal: "
                                    + resp.errorMessage());
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void clearForm() {
        if (nameField != null)            nameField.clear();
        if (targetField != null)          targetField.clear();
        if (monthsField != null)          monthsField.clear();
        if (rateField != null)            rateField.clear();
        if (storageLocationField != null) storageLocationField.clear();
        if (categoryChoice != null)       categoryChoice.setValue(Category.UMUM);
        if (storageTypeChoice != null)    storageTypeChoice.setValue("Bank");
        if (targetDatePicker != null)     targetDatePicker.setValue(null);
        if (editingLabel != null)         editingLabel.setVisible(false);
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
