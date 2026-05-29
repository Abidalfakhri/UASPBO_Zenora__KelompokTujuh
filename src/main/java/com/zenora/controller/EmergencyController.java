package com.zenora.controller;

import com.google.gson.JsonObject;
import com.zenora.model.Category;
import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.model.UserProfile;
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
import java.util.ResourceBundle;


public class EmergencyController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Dana Darurat"; }

    // ── Ringkasan profil (read-only) ───────────────────────────────────────
    @FXML private Label profileExpenseLabel, profileCapacityLabel,
                        profileStatusLabel, profileMonthsLabel, profileWarnLabel;

    // ── Kalkulator (hanya override cakupan bulan) ──────────────────────────
    @FXML private TextField monthsField;
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
        try { com.zenora.util.MoneyTextFormatter.attach(depositAmountField); } catch (Exception ignored) {}

        if (storageTypeChoice != null)
            storageTypeChoice.setItems(FXCollections.observableArrayList(
                    "Bank", "E-Wallet", "Investasi", "Tunai", "Lainnya"));

        if (depositDatePicker != null) depositDatePicker.setValue(LocalDate.now());

        wireHistoryTable();
        refreshProfileSummary();
        loadEmergencyGoal();
    }

    /** Sinkronkan ringkasan profil setiap kali halaman dibuka. */
    private void refreshProfileSummary() {
        UserProfile p = DataStore.getInstance().getProfile();
        double expense  = p.getMonthlyExpense();
        double capacity = p.effectiveCapacity();
        int months      = Math.max(1, p.getEmergencyMonths());

        if (profileExpenseLabel  != null) profileExpenseLabel.setText(expense  > 0 ? CurrencyFormatter.format(expense)  : "—");
        if (profileCapacityLabel != null) profileCapacityLabel.setText(capacity > 0 ? CurrencyFormatter.format(capacity) : "—");
        if (profileStatusLabel   != null) profileStatusLabel.setText(p.getHouseholdStatus());
        if (profileMonthsLabel   != null) profileMonthsLabel.setText(months + " bulan");

        if (profileWarnLabel != null) {
            if (expense <= 0) {
                profileWarnLabel.setText("⚠ Pengeluaran bulanan belum diisi di Profil. Lengkapi dulu agar kalkulasi akurat.");
            } else if (capacity <= 0) {
                profileWarnLabel.setText("⚠ Kapasitas menabung Anda 0 — pendapatan ≤ pengeluaran. Estimasi waktu tidak dapat dihitung.");
            } else {
                profileWarnLabel.setText("");
            }
        }
    }

    @FXML
    private void openProfile() {
        SceneNavigator.navigateTo("/com/zenora/fxml/Profile.fxml");
    }

    // ── Kalkulator ─────────────────────────────────────────────────────────

    @FXML
    private void calculate() {
        UserProfile p = DataStore.getInstance().getProfile();
        double expense  = p.getMonthlyExpense();
        double capacity = p.effectiveCapacity();

        if (expense <= 0) {
            resultArea.setText("⚠ Pengeluaran bulanan belum diisi di Profil. Buka Profil untuk melengkapi.");
            return;
        }

        int months = p.getEmergencyMonths();
        if (monthsField != null && !monthsField.getText().trim().isEmpty()) {
            InputValidator v = InputValidator.create();
            months = v.positiveInt(monthsField.getText(), "Jumlah bulan");
            if (v.hasErrors()) { resultArea.setText("⚠ " + v.errorMessage()); return; }
        }
        if (months < 1) months = 6;

        double need           = FinancialCalculator.emergencyFundNeeded(expense, months);
        int    monthsToBuild  = capacity > 0 ? (int) Math.ceil(need / capacity) : Integer.MAX_VALUE;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTIMASI DANA DARURAT ===\n\n");
        sb.append(String.format("Status                : %s%n", p.getHouseholdStatus()));
        sb.append(String.format("Cakupan bulan         : %d bulan%n", months));
        sb.append(String.format("Pengeluaran / bulan   : %s%n", CurrencyFormatter.format(expense)));
        sb.append(String.format("Kapasitas / bulan     : %s%n", CurrencyFormatter.format(capacity)));
        sb.append(String.format("%nDana darurat dibutuhkan : %s%n", CurrencyFormatter.format(need)));
        if (monthsToBuild < Integer.MAX_VALUE) {
            sb.append(String.format(
                    "%nDengan menabung %s/bulan,%nAnda butuh ± %d bulan (%.1f tahun).%n",
                    CurrencyFormatter.format(capacity), monthsToBuild, monthsToBuild / 12.0));
        } else {
            sb.append("\nKapasitas menabung 0 — tidak dapat memperkirakan waktu.\n");
        }
        sb.append("\nTips:\n");
        sb.append("• Simpan di instrumen likuid: tabungan / RDPU.\n");
        sb.append("• Pisahkan dari rekening harian.\n");
        sb.append("• Setelah lengkap, alihkan setoran ke goal lain.\n");
        sb.append("\n→ Gunakan panel kanan untuk mulai tracking setoran.");
        resultArea.setText(sb.toString());
    }

    // ── Goal Tracker ───────────────────────────────────────────────────────

    private void loadEmergencyGoal() {
        emergencyGoal = null;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getCategory() == Category.DARURAT) { emergencyGoal = g; break; }
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
            if (pct >= 1) goalStatusLabel.setText(" Dana darurat TERPENUHI!");
            else if (pct >= 0.5) goalStatusLabel.setText(" Lebih dari setengah jalan, teruskan!");
            else goalStatusLabel.setText("⚠ Dana darurat belum mencukupi, prioritaskan ini.");
        }

        if (historyTable != null) {
            List<Contribution> contribs = DataStore.getInstance().contributionsFor(emergencyGoal.getId());
            contribs.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            historyTable.setItems(FXCollections.observableArrayList(contribs));
        }
    }

    @FXML
    private void createGoal() {
        UserProfile p = DataStore.getInstance().getProfile();
        double expense = p.getMonthlyExpense();
        int    months  = Math.max(1, p.getEmergencyMonths());
        double target  = expense * months;

        if (target <= 0) {
            new Alert(Alert.AlertType.WARNING,
                    "Lengkapi pengeluaran bulanan di Profil terlebih dahulu.").showAndWait();
            return;
        }

        String name   = "Dana Darurat";
        String storageType = storageTypeChoice != null ? storageTypeChoice.getValue() : "Bank";
        String storageLocation = storageLocationField != null ? storageLocationField.getText().trim() : "";

        final double finalTarget   = target;
        final String finalStorage  = storageType;
        final String finalLocation = storageLocation;

        Thread t = new Thread(() -> {
            GoalRequest req = new GoalRequest(name, finalTarget, months, 0.0, 1,
                    "DARURAT", finalStorage, finalLocation);
            ApiClient.ApiResponse resp = ApiClient.post("/api/goals", req);
            Platform.runLater(() -> {
                Goal g = new Goal(name, finalTarget, months, 0.0, 1);
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

    private void wireHistoryTable() {
        if (historyTable == null) return;
        colDate.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDate())));
        colAmount.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getAmount()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color: transparent;");
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
        colNote.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNote()));
        historyTable.setPlaceholder(new Label("Belum ada setoran."));
    }

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
