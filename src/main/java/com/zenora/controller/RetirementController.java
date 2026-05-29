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
import java.util.Optional;
import java.util.ResourceBundle;


public class RetirementController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Dana Pensiun"; }

    // ── Ringkasan profil (read-only) ───────────────────────────────────────
    @FXML private Label profileAgeLabel, profileInflationLabel,
                        profileCapacityLabel, profileWarnLabel;

    // ── Kalkulator ─────────────────────────────────────────────────────────
    @FXML private TextField retirementAgeField, yearsInRetirementField,
            monthlyNeedField, returnPreField, returnPostField;
    @FXML private TextArea resultArea;

    // ── Goal Tracker ───────────────────────────────────────────────────────
    @FXML private Label goalNameLabel, goalTargetLabel, goalSavedLabel,
                        goalPctLabel, goalStorageLabel, goalStatusLabel,
                        goalMonthlyNeededLabel;
    @FXML private ProgressBar goalProgress;
    @FXML private TextField depositAmountField, depositNoteField;
    @FXML private DatePicker depositDatePicker;
    @FXML private ChoiceBox<String> storageTypeChoice;
    @FXML private TextField storageLocationField, goalTargetInputField;
    @FXML private TableView<Contribution> historyTable;
    @FXML private TableColumn<Contribution, String> colDate, colNote;
    @FXML private TableColumn<Contribution, Number> colAmount;
    @FXML private Button depositButton, createGoalButton;

    private Goal retirementGoal = null;
    private double lastCalculatedTarget = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try { com.zenora.util.MoneyTextFormatter.attach(depositAmountField); } catch (Exception ignored) {}
        try { com.zenora.util.MoneyTextFormatter.attach(goalTargetInputField); } catch (Exception ignored) {}
        try { com.zenora.util.MoneyTextFormatter.attach(monthlyNeedField); } catch (Exception ignored) {}

        if (storageTypeChoice != null)
            storageTypeChoice.setItems(FXCollections.observableArrayList(
                    "Bank", "E-Wallet", "Investasi", "Tunai", "Lainnya"));

        if (depositDatePicker != null) depositDatePicker.setValue(LocalDate.now());

        // Default asumsi return
        if (returnPreField  != null && returnPreField.getText().isBlank())  returnPreField.setText("8.0");
        if (returnPostField != null && returnPostField.getText().isBlank()) returnPostField.setText("5.0");
        if (yearsInRetirementField != null && yearsInRetirementField.getText().isBlank())
            yearsInRetirementField.setText("20");

        wireHistoryTable();
        refreshProfileSummary();
        loadRetirementGoal();
    }

    private void refreshProfileSummary() {
        UserProfile p = DataStore.getInstance().getProfile();
        int    age       = p.getAge();
        double inflation = p.getInflationPct();
        double capacity  = p.effectiveCapacity();

        if (profileAgeLabel       != null) profileAgeLabel.setText(age > 0 ? age + " tahun" : "—");
        if (profileInflationLabel != null) profileInflationLabel.setText(String.format("%.1f%% / thn", inflation));
        if (profileCapacityLabel  != null) profileCapacityLabel.setText(capacity > 0 ? CurrencyFormatter.format(capacity) : "—");

        if (profileWarnLabel != null) {
            if (age <= 0) {
                profileWarnLabel.setText("⚠ Usia belum diisi di Profil. Lengkapi dulu untuk hitungan akurat.");
            } else if (capacity <= 0) {
                profileWarnLabel.setText("⚠ Kapasitas menabung 0 — periksa pendapatan & pengeluaran di Profil.");
            } else {
                profileWarnLabel.setText("");
            }
        }

        // Saran default umur pensiun
        if (retirementAgeField != null && retirementAgeField.getText().isBlank() && age > 0) {
            retirementAgeField.setText(String.valueOf(Math.max(age + 1, 55)));
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
        int    currentAge = p.getAge();
        double inflation  = p.getInflationPct();

        if (currentAge <= 0) {
            resultArea.setText("⚠ Usia belum diisi di Profil. Buka Profil untuk melengkapi.");
            return;
        }

        InputValidator v = InputValidator.create();
        int    retirementAge     = v.positiveInt(retirementAgeField.getText(),     "Usia pensiun");
        int    yearsInRetirement = v.positiveInt(yearsInRetirementField.getText(), "Lama pensiun (tahun)");
        double monthlyNeed       = v.positiveDouble(monthlyNeedField.getText(),    "Kebutuhan bulanan");
        double rPre              = v.positiveDouble(returnPreField.getText(),      "Return pra-pensiun");
        double rPost             = v.positiveDouble(returnPostField.getText(),     "Return pasca-pensiun");
        if (v.hasErrors()) { resultArea.setText("⚠ " + v.errorMessage()); return; }

        if (retirementAge <= currentAge) {
            resultArea.setText("⚠ Usia pensiun harus lebih besar dari usia saat ini (" + currentAge + ").");
            return;
        }

        int    yearsToRetire   = retirementAge - currentAge;
        double fundNeeded      = FinancialCalculator.retirementFundNeeded(
                monthlyNeed, inflation, yearsToRetire, yearsInRetirement, rPost);
        double monthlyAtRetire = FinancialCalculator.inflateAmount(monthlyNeed, inflation, yearsToRetire);
        double monthlyContrib  = FinancialCalculator.requiredMonthlyContribution(
                fundNeeded, rPre, yearsToRetire * 12);

        lastCalculatedTarget = fundNeeded;

        StringBuilder sb = new StringBuilder();
        sb.append("=== ESTIMASI DANA PENSIUN ===\n\n");
        sb.append(String.format("Usia saat ini (Profil)        : %d tahun%n", currentAge));
        sb.append(String.format("Usia pensiun target           : %d tahun%n", retirementAge));
        sb.append(String.format("Tahun menuju pensiun          : %d tahun%n", yearsToRetire));
        sb.append(String.format("Kebutuhan bulanan (kini)      : %s%n", CurrencyFormatter.format(monthlyNeed)));
        sb.append(String.format("Kebutuhan bulanan saat pensiun: %s%n", CurrencyFormatter.format(monthlyAtRetire)));
        sb.append(String.format("%nTotal dana pensiun dibutuhkan : %s%n", CurrencyFormatter.format(fundNeeded)));
        sb.append(String.format("%n→ Perlu menabung sekitar:%n   %s per bulan%n   selama %d tahun (%d bulan)%n",
                CurrencyFormatter.format(monthlyContrib), yearsToRetire, yearsToRetire * 12));
        sb.append(String.format("%nAsumsi: return pra %.1f%%/thn, pasca %.1f%%/thn, inflasi %.1f%%/thn.",
                rPre, rPost, inflation));
        sb.append("\n\n→ Klik \"Buat Goal Pensiun\" di panel kanan untuk mulai tracking.");
        resultArea.setText(sb.toString());

        if (goalMonthlyNeededLabel != null)
            goalMonthlyNeededLabel.setText(
                    "Setoran bulanan ideal: " + CurrencyFormatter.format(monthlyContrib));
        if (goalTargetInputField != null && goalTargetInputField.getText().isBlank())
            goalTargetInputField.setText(String.valueOf((long) fundNeeded));
    }

    // ── Goal Tracker ───────────────────────────────────────────────────────

    private void loadRetirementGoal() {
        retirementGoal = null;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getCategory() == Category.PENSIUN) { retirementGoal = g; break; }
        }
        refreshTrackerUI();
    }

    private void refreshTrackerUI() {
        boolean hasGoal = retirementGoal != null;
        if (depositButton    != null) depositButton.setDisable(!hasGoal);
        if (createGoalButton != null) createGoalButton.setDisable(hasGoal);

        if (!hasGoal) {
            if (goalNameLabel    != null) goalNameLabel.setText("Belum ada goal Dana Pensiun.");
            if (goalTargetLabel  != null) goalTargetLabel.setText("-");
            if (goalSavedLabel   != null) goalSavedLabel.setText("-");
            if (goalPctLabel     != null) goalPctLabel.setText("0%");
            if (goalStorageLabel != null) goalStorageLabel.setText("-");
            if (goalStatusLabel  != null) goalStatusLabel.setText("Hitung estimasi, lalu buat goal Dana Pensiun.");
            if (goalProgress     != null) goalProgress.setProgress(0);
            if (historyTable     != null) historyTable.setItems(FXCollections.observableArrayList());
            return;
        }

        DataStore.getInstance().recomputeAllProgress();
        double saved  = retirementGoal.getCurrentSaving();
        double target = retirementGoal.getTargetAmount();
        double pct    = target == 0 ? 0 : Math.min(1, saved / target);

        if (goalNameLabel    != null) goalNameLabel.setText(retirementGoal.getName());
        if (goalTargetLabel  != null) goalTargetLabel.setText(CurrencyFormatter.format(target));
        if (goalSavedLabel   != null) goalSavedLabel.setText(CurrencyFormatter.format(saved));
        if (goalPctLabel     != null) goalPctLabel.setText(String.format("%.1f%%", pct * 100));
        if (goalProgress     != null) goalProgress.setProgress(pct);
        if (goalStorageLabel != null) goalStorageLabel.setText(
                retirementGoal.getStorageType() + " — " + retirementGoal.getStorageLocation());
        if (goalStatusLabel  != null) {
            if (pct >= 1)         goalStatusLabel.setText("Dana pensiun TERPENUHI! Luar biasa!");
            else if (pct >= 0.75) goalStatusLabel.setText("Hampir sampai, pertahankan konsistensi!");
            else if (pct >= 0.5)  goalStatusLabel.setText("Sudah setengah jalan, terus lanjutkan!");
            else if (pct >= 0.25) goalStatusLabel.setText("Progres bagus, jangan berhenti!");
            else                  goalStatusLabel.setText("Baru mulai — konsistensi adalah kuncinya.");
        }

        if (historyTable != null) {
            List<Contribution> contribs = DataStore.getInstance().contributionsFor(retirementGoal.getId());
            contribs.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            historyTable.setItems(FXCollections.observableArrayList(contribs));
        }
    }

    @FXML
    private void createGoal() {
        String name   = "Dana Pensiun";
        double target = lastCalculatedTarget;

        if (goalTargetInputField != null && !goalTargetInputField.getText().isBlank()) {
            try { target = Double.parseDouble(goalTargetInputField.getText().replace(",","").replace(".","")); }
            catch (Exception ignored) {}
        }

        if (target <= 0) {
            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle("Target Dana Pensiun");
            dialog.setHeaderText("Masukkan target dana pensiun (Rp):");
            dialog.setContentText("Target (Rp):");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) return;
            try { target = Double.parseDouble(result.get().replace(",","").replace(".","")); }
            catch (Exception e) { new Alert(Alert.AlertType.WARNING, "Angka tidak valid.").showAndWait(); return; }
        }

        String storageType     = storageTypeChoice     != null ? storageTypeChoice.getValue()     : "Investasi";
        String storageLocation = storageLocationField  != null ? storageLocationField.getText().trim() : "";

        // Ambil tahun ke pensiun dari Profil (usia) + form
        UserProfile p = DataStore.getInstance().getProfile();
        int yearsToRetire = 30;
        try {
            int ca = p.getAge();
            int ra = Integer.parseInt(retirementAgeField.getText().trim());
            if (ra > ca && ca > 0) yearsToRetire = ra - ca;
        } catch (Exception ignored) {}

        final double finalTarget  = target;
        final String finalStorage = storageType;
        final String finalLoc     = storageLocation;
        final int    finalMonths  = yearsToRetire * 12;

        Thread t = new Thread(() -> {
            GoalRequest req = new GoalRequest(name, finalTarget, finalMonths, 8.0, 1,
                    "PENSIUN", finalStorage, finalLoc);
            ApiClient.ApiResponse resp = ApiClient.post("/api/goals", req);
            Platform.runLater(() -> {
                Goal g = new Goal(name, finalTarget, finalMonths, 8.0, 1);
                g.setCategory(Category.PENSIUN);
                g.setStorageType(finalStorage);
                g.setStorageLocation(finalLoc);
                if (resp.isSuccess()) {
                    try {
                        JsonObject obj = ApiClient.parseObject(resp.body);
                        if (obj.has("id")) g.setId(obj.get("id").getAsString());
                    } catch (Exception ignored) {}
                }
                DataStore.getInstance().getGoals().add(g);
                retirementGoal = g;
                refreshTrackerUI();
                new Alert(Alert.AlertType.INFORMATION, "Goal Dana Pensiun berhasil dibuat!").showAndWait();
            });
        });
        t.setDaemon(true); t.start();
    }

    @FXML
    private void deposit() {
        if (retirementGoal == null) return;
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

        final String goalId = retirementGoal.getId();
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
