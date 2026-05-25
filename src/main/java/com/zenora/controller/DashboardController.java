package com.zenora.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zenora.app.AppSession;
import com.zenora.model.Category;
import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.model.UserProfile;
import com.zenora.service.ApiClient;
import com.zenora.service.CsvExporter;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

/**
 * DashboardController — diperbarui untuk sinkronisasi dengan REST API backend.
 *
 * Perubahan:
 *   - initialize → load data dari /api/goals, /api/profile, /api/contributions
 *   - Data dimasukkan ke DataStore lokal agar modul lain tetap berjalan
 *   - Greeting menggunakan username dari AppSession
 *   - logout() → hapus sesi dan kembali ke Login.fxml
 */
public class DashboardController implements Initializable {

    @FXML private Label greetingLabel, capacityLabel, savedLabel, targetLabel,
            progressLabel, emergencyLabel, goalsCountLabel, contribCountLabel,
            efStatusLabel, runwayLabel, sidebarCapLabel;
    @FXML private ProgressBar overallProgress, efProgress;
    @FXML private VBox alertsBox;
    @FXML private PieChart allocationChart;
    @FXML private TableView<Goal> recentGoalsTable;
    @FXML private TableColumn<Goal, String> colGoalName, colGoalCat;
    @FXML private TableColumn<Goal, Number> colGoalTarget, colGoalSaved, colGoalPct;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        wireTable();
        refresh();          // tampilkan cache lokal dulu
        syncFromBackend();  // lalu sync dari database
    }

    // ── Backend sync ──────────────────────────────────────────────────────

    private void syncFromBackend() {
        Thread t = new Thread(() -> {
            try {
                loadProfileFromBackend();
                loadGoalsFromBackend();
                loadContributionsFromBackend();
            } catch (Exception e) {
                System.err.println("[Dashboard] Sync error: " + e.getMessage());
            }
            Platform.runLater(this::refresh);
        });
        t.setDaemon(true);
        t.start();
    }

    private void loadProfileFromBackend() {
        ApiClient.ApiResponse resp = ApiClient.get("/api/profile");
        if (!resp.isSuccess() || resp.status == 204 || resp.body.isBlank()) return;
        try {
            JsonObject obj = ApiClient.parseObject(resp.body);
            UserProfile p = DataStore.getInstance().getProfile();
            if (obj.has("id"))              AppSession.getInstance().setProfileId(obj.get("id").getAsString());
            if (obj.has("name"))            p.setName(obj.get("name").getAsString());
            if (obj.has("age"))             p.setAge(obj.get("age").getAsInt());
            if (obj.has("monthlyIncome"))   p.setMonthlyIncome(obj.get("monthlyIncome").getAsDouble());
            if (obj.has("monthlyExpense"))  p.setMonthlyExpense(obj.get("monthlyExpense").getAsDouble());
            if (obj.has("emergencyMonths")) p.setEmergencyMonths(obj.get("emergencyMonths").getAsInt());
            if (obj.has("householdStatus")) p.setHouseholdStatus(obj.get("householdStatus").getAsString());
            if (obj.has("inflationPct"))    p.setInflationPct(obj.get("inflationPct").getAsDouble());
        } catch (Exception e) {
            System.err.println("[Dashboard] Profile parse error: " + e.getMessage());
        }
    }

    private void loadGoalsFromBackend() {
        ApiClient.ApiResponse resp = ApiClient.get("/api/goals");
        if (!resp.isSuccess()) return;
        try {
            JsonArray arr = ApiClient.parseArray(resp.body);
            DataStore ds = DataStore.getInstance();
            ds.getGoals().clear();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                Goal g = new Goal();
                if (obj.has("id"))            g.setId(obj.get("id").getAsString());
                if (obj.has("name"))          g.setName(obj.get("name").getAsString());
                if (obj.has("targetAmount"))  g.setTargetAmount(obj.get("targetAmount").getAsDouble());
                if (obj.has("currentSaving")) g.setCurrentSaving(obj.get("currentSaving").getAsDouble());
                if (obj.has("months"))        g.setMonths(obj.get("months").getAsInt());
                if (obj.has("interestRate"))  g.setInterestRate(obj.get("interestRate").getAsDouble());
                if (obj.has("priority"))      g.setPriority(obj.get("priority").getAsInt());
                if (obj.has("category")) {
                    try { g.setCategory(Category.valueOf(obj.get("category").getAsString())); }
                    catch (Exception ex) { g.setCategory(Category.UMUM); }
                }
                ds.getGoals().add(g);
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Goals parse error: " + e.getMessage());
        }
    }

    private void loadContributionsFromBackend() {
        ApiClient.ApiResponse resp = ApiClient.get("/api/contributions");
        if (!resp.isSuccess()) return;
        try {
            JsonArray arr = ApiClient.parseArray(resp.body);
            DataStore ds = DataStore.getInstance();
            ds.getContributions().clear();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                Contribution c = new Contribution();
                if (obj.has("id"))     c.setId(obj.get("id").getAsString());
                if (obj.has("goalId")) c.setGoalId(obj.get("goalId").getAsString());
                if (obj.has("amount")) c.setAmount(obj.get("amount").getAsDouble());
                if (obj.has("note") && !obj.get("note").isJsonNull()) c.setNote(obj.get("note").getAsString());
                if (obj.has("date") && !obj.get("date").isJsonNull()) {
                    try { c.setDate(LocalDate.parse(obj.get("date").getAsString())); }
                    catch (Exception ex) { c.setDate(LocalDate.now()); }
                }
                ds.getContributions().add(c);
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Contributions parse error: " + e.getMessage());
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────

    private void wireTable() {
        colGoalName.setCellValueFactory(c -> c.getValue().nameProperty());
        colGoalCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().getLabel()));
        colGoalTarget.setCellValueFactory(c -> c.getValue().targetAmountProperty());
        colGoalSaved.setCellValueFactory(c -> c.getValue().currentSavingProperty());
        colGoalPct.setCellValueFactory(c ->
                new javafx.beans.property.SimpleDoubleProperty(c.getValue().getProgressPercent()));
        formatCurrencyCol(colGoalTarget);
        formatCurrencyCol(colGoalSaved);
        colGoalPct.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(""); setStyle(""); return; }
                double pct = v.doubleValue();
                setText(String.format("%.1f%%", pct));
                if (pct >= 100) setStyle("-fx-text-fill: #34D399; -fx-font-weight: 700;");
                else if (pct >= 50) setStyle("-fx-text-fill: #FBBF24;");
                else setStyle("-fx-text-fill: #F87171;");
            }
        });
    }

    private void formatCurrencyCol(TableColumn<Goal, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
    }

    private void refresh() {
        DataStore ds = DataStore.getInstance();
        ds.recomputeAllProgress();
        UserProfile p = ds.getProfile();

        String displayName = p.getName().isBlank()
                ? AppSession.getInstance().getUsername()
                : p.getName();
        greetingLabel.setText("Halo, " + displayName + " 👋");

        double cap = p.effectiveCapacity();
        String capStr = CurrencyFormatter.format(cap) + " / bln";
        capacityLabel.setText(capStr);
        if (sidebarCapLabel != null) sidebarCapLabel.setText("Kapasitas: " + capStr);

        double totalSaved = 0, totalTarget = 0;
        for (Goal g : ds.getGoals()) {
            totalSaved += g.getCurrentSaving();
            totalTarget += g.getTargetAmount();
        }
        savedLabel.setText(CurrencyFormatter.format(totalSaved));
        targetLabel.setText(CurrencyFormatter.format(totalTarget));
        double pct = totalTarget == 0 ? 0 : Math.min(1, totalSaved / totalTarget);
        overallProgress.setProgress(pct);
        progressLabel.setText(String.format("%.1f%%", pct * 100));

        if (pct >= 0.8) overallProgress.getStyleClass().setAll("zn-progress", "zn-progress-green");
        else if (pct >= 0.4) overallProgress.getStyleClass().setAll("zn-progress");
        else overallProgress.getStyleClass().setAll("zn-progress", "zn-progress-amber");

        goalsCountLabel.setText(String.valueOf(ds.getGoals().size()));
        contribCountLabel.setText(String.valueOf(ds.getContributions().size()));

        double efTarget = p.recommendedEmergencyFund();
        double efSaved = 0;
        for (Goal g : ds.getGoals()) {
            if (g.getCategory() != null && g.getCategory().name().equals("DARURAT")) {
                efSaved += g.getCurrentSaving();
            }
        }
        emergencyLabel.setText(CurrencyFormatter.format(efSaved) + " / " + CurrencyFormatter.format(efTarget));
        double efPct = efTarget == 0 ? 0 : Math.min(1, efSaved / efTarget);
        efProgress.setProgress(efPct);

        if (efTarget == 0) efStatusLabel.setText("Belum diatur — isi profil dulu.");
        else if (efSaved >= efTarget) efStatusLabel.setText("✓ Tercukupi (" + p.getEmergencyMonths() + " bulan)");
        else efStatusLabel.setText(String.format("%.0f%% — masih perlu ditingkatkan", efPct * 100));

        double monthlyExpense = p.getMonthlyExpense();
        if (monthlyExpense > 0)
            runwayLabel.setText(String.format("≈ %.1f bulan runway", efSaved / monthlyExpense));
        else
            runwayLabel.setText("Isi pengeluaran di profil.");

        allocationChart.getData().clear();
        for (Goal g : ds.getGoals()) {
            if (g.getTargetAmount() > 0)
                allocationChart.getData().add(new PieChart.Data(g.getName(), g.getTargetAmount()));
        }

        alertsBox.getChildren().clear();
        if (ds.getGoals().isEmpty())
            alertsBox.getChildren().add(chip("info", "💡 Belum ada goal. Buka Goal Planning untuk membuat goal pertama Anda."));
        if (p.getMonthlyIncome() == 0)
            alertsBox.getChildren().add(chip("warn", "⚠ Profil belum diisi — kapasitas menabung belum dapat dihitung."));
        if (efTarget > 0 && efSaved < efTarget * 0.5)
            alertsBox.getChildren().add(chip("warn", "⚠ Dana darurat masih di bawah 50% target. Prioritaskan ini!"));

        LocalDate today = LocalDate.now();
        for (Goal g : ds.getGoals()) {
            if (g.getProgressPercent() >= 100) continue;
            LocalDate last = lastContributionDate(g.getId());
            if (last == null) {
                alertsBox.getChildren().add(chip("info",
                        "💡 Belum ada setoran untuk \"" + g.getName() + "\". Catat setoran pertama Anda."));
            } else if (ChronoUnit.DAYS.between(last, today) > 35) {
                alertsBox.getChildren().add(chip("warn",
                        "⏰ Goal \"" + g.getName() + "\" belum disetor selama "
                        + ChronoUnit.DAYS.between(last, today) + " hari."));
            }
        }

        if (alertsBox.getChildren().isEmpty())
            alertsBox.getChildren().add(chip("ok", "✓ Semua sehat. Lanjutkan disiplin menabung bulan ini!"));

        recentGoalsTable.setItems(ds.getGoals());
    }

    private LocalDate lastContributionDate(String goalId) {
        LocalDate last = null;
        for (Contribution c : DataStore.getInstance().getContributions()) {
            if (!goalId.equals(c.getGoalId())) continue;
            if (last == null || c.getDate().isAfter(last)) last = c.getDate();
        }
        return last;
    }

    private Label chip(String kind, String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(7, 12, 7, 12));
        l.getStyleClass().add(switch (kind) {
            case "warn"  -> "zn-chip-warn";
            case "ok"    -> "zn-chip-ok";
            case "error" -> "zn-chip-error";
            default      -> "zn-chip-info";
        });
        return l;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML private void stayDashboard()     { syncFromBackend(); }
    @FXML private void openMenu()          { SceneNavigator.navigateTo("/com/zenora/fxml/MainMenu.fxml"); }
    @FXML private void openProfile()       { SceneNavigator.navigateTo("/com/zenora/fxml/Profile.fxml"); }
    @FXML private void openContributions() { SceneNavigator.navigateTo("/com/zenora/fxml/Contribution.fxml"); }
    @FXML private void openReports()       { SceneNavigator.navigateTo("/com/zenora/fxml/Reports.fxml"); }
    @FXML private void openGoal()          { SceneNavigator.navigateTo("/com/zenora/fxml/Goal.fxml"); }
    @FXML private void openMulti()         { SceneNavigator.navigateTo("/com/zenora/fxml/MultiGoal.fxml"); }
    @FXML private void openEmergency()     { SceneNavigator.navigateTo("/com/zenora/fxml/Emergency.fxml"); }
    @FXML private void openRetirement()    { SceneNavigator.navigateTo("/com/zenora/fxml/Retirement.fxml"); }
    @FXML private void openWhatIf()        { SceneNavigator.navigateTo("/com/zenora/fxml/WhatIf.fxml"); }
    @FXML private void refreshClick()      { syncFromBackend(); }

    /** Logout — hapus sesi, kembali ke Login. */
    @FXML
    private void logout() {
        AppSession.getInstance().clearSession();
        DataStore.getInstance().getGoals().clear();
        DataStore.getInstance().getContributions().clear();
        SceneNavigator.navigateTo("/com/zenora/fxml/Login.fxml");
    }

    @FXML
    private void exportCsv() {
        DirectoryChooser ch = new DirectoryChooser();
        ch.setTitle("Pilih folder ekspor CSV");
        File dir = ch.showDialog(SceneNavigator.getPrimaryStage());
        if (dir == null) return;
        try {
            Path path = CsvExporter.exportAll(dir.toPath());
            showInfo("Export Berhasil", "CSV diekspor ke:\n" + path.toAbsolutePath());
        } catch (Exception e) {
            showError("Export Gagal", e.getMessage());
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
