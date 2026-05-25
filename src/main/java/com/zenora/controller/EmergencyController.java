package com.zenora.controller;

import com.google.gson.JsonObject;
import com.zenora.model.Category;
import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.ApiClient;
import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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

/**
 * EmergencyController — Dana Darurat Goal Tracker + Kalkulator.
 *
 * Dua fungsi utama:
 *   1. Kalkulator: estimasi berapa dana darurat yang dibutuhkan.
 *   2. Goal Tracker: simpan progress & setor ke goal DARURAT di database.
 */
public class EmergencyController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Dana Darurat"; }

    // ── Kalkulator ─────────────────────────────────────────────────────────
    @FXML private TextField expenseField, monthsField, capacityField;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private TextArea resultArea;

    // ── Goal Tracker ───────────────────────────────────────────────────────
    @FXML private Label goalNameLabel, goalTargetLabel, goalSavedLabel,
                        goalPctLabel, goalStorageLabel, goalStatusLabel;
    @FXML private ProgressBar goalProgress;
    @FXML private TextField depositAmountField, depositNoteField;
    @FXML private DatePicker depositDatePicker;
    @FXML private ChoiceBox<String> storageTypeChoice;
    @FXML private TextField storageLocationField;
    @FXML private TableView<Contribution> historyTable;
    @FXML private TableColumn<Contribution, String> colDate, colNote;
    @FXML private TableColumn<Contribution, Number> colAmount;
    @FXML private Button depositButton, createGoalButton;

    private Goal emergencyGoal = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Format ribuan untuk input uang
        try { com.zenora.util.MoneyTextFormatter.attach(expenseField); } catch (Exception ignored) {}
        try { com.zenora.util.MoneyTextFormatter.attach(capacityField); } catch (Exception ignored) {}
        try { com.zenora.util.MoneyTextFormatter.attach(depositAmountField); } catch (Exception ignored) {}

        statusChoice.setItems(FXCollections.observableArrayList(
                "Single (3 bulan)",
                "Menikah tanpa anak (6 bulan)",
                "Menikah + anak (12 bulan)",
                "Freelancer / Self-employed (12 bulan)"));
        statusChoice.getSelectionModel().selectFirst();

        if (storageTypeChoice != null)
            storageTypeChoice.setItems(FXCollections.observableArrayList(
                    "Bank", "E-Wallet", "Investasi", "Tunai", "Lainnya"));

        if (depositDatePicker != null) depositDatePicker.setValue(LocalDate.now());

        wireHistoryTable();
        loadEmergencyGoal();
    }

    // ── Kalkulator ─────────────────────────────────────────────────────────

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

        double need           = FinancialCalculator.emergencyFundNeeded(expense, months);
        int monthsToBuild     = capacity > 0 ? (int) Math.ceil(need / capacity) : Integer.MAX_VALUE;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTIMASI DANA DARURAT ===\n\n");
        sb.append(String.format("Status                      : %s%n", statusChoice.getValue()));
        sb.append(String.format("Cakupan bulan               : %d bulan%n", months));
        sb.append(String.format("Pengeluaran bulanan         : %s%n", CurrencyFormatter.format(expense)));
        sb.append(String.format("%nDana darurat dibutuhkan     : %s%n", CurrencyFormatter.format(need)));
        if (monthsToBuild < Integer.MAX_VALUE) {
            sb.append(String.format(
                    "%nDengan menabung %s/bulan,%nAnda butuh sekitar %d bulan (%.1f tahun).%n",
                    CurrencyFormatter.format(capacity), monthsToBuild, monthsToBuild / 12.0));
        }
        sb.append("\nTips:\n");
        sb.append("• Simpan di instrumen likuid: tabungan / reksa dana pasar uang.\n");
        sb.append("• Pisahkan dari rekening harian agar tidak terpakai.\n");
        sb.append("• Setelah terkumpul, lanjutkan ke goal lain (multi-goal planning).\n");
        sb.append("\n→ Gunakan panel kanan untuk mulai tracking setoran dana darurat Anda.");
        resultArea.setText(sb.toString());
    }

    // ── Goal Tracker ───────────────────────────────────────────────────────

    /** Cari goal dengan kategori DARURAT di DataStore. */
    private void loadEmergencyGoal() {
        emergencyGoal = null;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getCategory() == Category.DARURAT) {
                emergencyGoal = g;
                break;
            }
        }
        refreshTrackerUI();
    }

    private void refreshTrackerUI() {
        boolean hasGoal = emergencyGoal != null;
        if (depositButton  != null) depositButton.setDisable(!hasGoal);
        if (createGoalButton != null) createGoalButton.setDisable(hasGoal);

        if (!hasGoal) {
            if (goalNameLabel   != null) goalNameLabel.setText("Belum ada goal Dana Darurat.");
            if (goalTargetLabel != null) goalTargetLabel.setText("-");
            if (goalSavedLabel  != null) goalSavedLabel.setText("-");
            if (goalPctLabel    != null) goalPctLabel.setText("0%");
            if (goalStorageLabel!= null) goalStorageLabel.setText("-");
            if (goalStatusLabel != null) goalStatusLabel.setText("Buat goal Dana Darurat terlebih dahulu.");
            if (goalProgress    != null) goalProgress.setProgress(0);
            if (historyTable    != null) historyTable.setItems(FXCollections.observableArrayList());
            return;
        }

        DataStore.getInstance().recomputeAllProgress();
        double saved  = emergencyGoal.getCurrentSaving();
        double target = emergencyGoal.getTargetAmount();
        double pct    = target == 0 ? 0 : Math.min(1, saved / target);

        if (goalNameLabel   != null) goalNameLabel.setText(emergencyGoal.getName());
        if (goalTargetLabel != null) goalTargetLabel.setText(CurrencyFormatter.format(target));
        if (goalSavedLabel  != null) goalSavedLabel.setText(CurrencyFormatter.format(saved));
        if (goalPctLabel    != null) goalPctLabel.setText(String.format("%.1f%%", pct * 100));
        if (goalProgress    != null) goalProgress.setProgress(pct);
        if (goalStorageLabel!= null) goalStorageLabel.setText(
                emergencyGoal.getStorageType() + " — " + emergencyGoal.getStorageLocation());
        if (goalStatusLabel != null) {
            if (pct >= 1) goalStatusLabel.setText("✅ Dana darurat TERPENUHI!");
            else if (pct >= 0.5) goalStatusLabel.setText("⚡ Lebih dari setengah jalan, teruskan!");
            else goalStatusLabel.setText("⚠ Dana darurat belum mencukupi, prioritaskan ini.");
        }

        // Isi riwayat setoran untuk goal ini
        if (historyTable != null) {
            List<Contribution> contribs = DataStore.getInstance().contributionsFor(emergencyGoal.getId());
            contribs.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            historyTable.setItems(FXCollections.observableArrayList(contribs));
        }
    }

    @FXML
    private void createGoal() {
        // Ambil nama dan target dari form kalkulator (jika sudah diisi), atau buat default
        String name   = "Dana Darurat";
        double target = 0;
        if (!expenseField.getText().isBlank()) {
            try {
                double exp = Double.parseDouble(expenseField.getText().replace(",", "").replace(".", ""));
                int m = switch (statusChoice.getSelectionModel().getSelectedIndex()) {
                    case 1 -> 6; case 2, 3 -> 12; default -> 3;
                };
                target = exp * m;
            } catch (Exception ignored) {}
        }

        if (target == 0) {
            // Minta target manual
            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle("Target Dana Darurat");
            dialog.setHeaderText("Masukkan target dana darurat (Rp):");
            dialog.setContentText("Target (Rp):");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) return;
            try { target = Double.parseDouble(result.get().replace(",", "").replace(".", "")); }
            catch (Exception e) { new Alert(Alert.AlertType.WARNING, "Angka tidak valid.").showAndWait(); return; }
        }

        String storageType = storageTypeChoice != null ? storageTypeChoice.getValue() : "Bank";
        String storageLocation = storageLocationField != null ? storageLocationField.getText().trim() : "";

        final double finalTarget = target;
        final String finalStorage = storageType;
        final String finalLocation = storageLocation;

        Thread t = new Thread(() -> {
            GoalRequest req = new GoalRequest(name, finalTarget, 12, 0.0, 1,
                    "DARURAT", finalStorage, finalLocation);
            ApiClient.ApiResponse resp = ApiClient.post("/api/goals", req);
            Platform.runLater(() -> {
                Goal g = new Goal(name, finalTarget, 12, 0.0, 1);
                g.setCategory(Category.DARURAT);
                g.setStorageType(finalStorage);
                g.setStorageLocation(finalLocation);
                if (resp.isSuccess()) {
                    try {
                        JsonObject obj = ApiClient.parseObject(resp.body);
                        if (obj.has("id")) g.setId(obj.get("id").getAsString());
                    } catch (Exception ignored) {}
                }
                DataStore.getInstance().getGoals().add(g);
                emergencyGoal = g;
                refreshTrackerUI();
                new Alert(Alert.AlertType.INFORMATION, "Goal Dana Darurat berhasil dibuat!").showAndWait();
            });
        });
        t.setDaemon(true); t.start();
    }

    @FXML
    private void deposit() {
        if (emergencyGoal == null) return;
        InputValidator v = InputValidator.create();
        double amt = v.positiveDouble(depositAmountField.getText(), "Nominal setoran");
        if (v.hasErrors()) { new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait(); return; }

        LocalDate date = depositDatePicker != null && depositDatePicker.getValue() != null
                ? depositDatePicker.getValue() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            new Alert(Alert.AlertType.WARNING, "Tanggal setoran tidak boleh di masa depan.").showAndWait(); return;
        }
        String note = depositNoteField != null ? depositNoteField.getText().trim() : "";
        if (depositAmountField != null) depositAmountField.clear();
        if (depositNoteField   != null) depositNoteField.clear();

        final String goalId = emergencyGoal.getId();
        Thread t = new Thread(() -> {
            ContribRequest req = new ContribRequest(goalId, amt, date.toString(), note);
            ApiClient.ApiResponse resp = ApiClient.post("/api/contributions", req);
            Platform.runLater(() -> {
                Contribution c;
                if (resp.isSuccess()) {
                    try {
                        JsonObject obj = ApiClient.parseObject(resp.body);
                        String id = obj.has("id") ? obj.get("id").getAsString() : null;
                        c = new Contribution(goalId, date, amt, note);
                        if (id != null) c.setId(id);
                    } catch (Exception ex) {
                        c = new Contribution(goalId, date, amt, note);
                    }
                } else {
                    c = new Contribution(goalId, date, amt, note);
                }
                DataStore.getInstance().getContributions().add(c);
                DataStore.getInstance().recomputeAllProgress();
                refreshTrackerUI();
            });
        });
        t.setDaemon(true); t.start();
    }

    // ── Tabel riwayat ──────────────────────────────────────────────────────
    private void wireHistoryTable() {
        if (historyTable == null) return;
        colDate.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDate())));
        colAmount.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getAmount()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
        colNote.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNote()));
        historyTable.setPlaceholder(new Label("Belum ada setoran."));
    }

    // ── Inner DTOs ─────────────────────────────────────────────────────────
    private static class GoalRequest {
        String name; double targetAmount; int months; double interestRate;
        int priority; String category; String storageType; String storageLocation;
        GoalRequest(String n, double t, int m, double r, int p, String cat, String sType, String sLoc) {
            name=n; targetAmount=t; months=m; interestRate=r; priority=p;
            category=cat; storageType=sType; storageLocation=sLoc;
        }
    }
    private static class ContribRequest {
        String goalId; double amount; String date; String note;
        ContribRequest(String g, double a, String d, String n) { goalId=g; amount=a; date=d; note=n; }
    }
}
