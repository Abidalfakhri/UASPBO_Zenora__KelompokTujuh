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
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.prefs.Preferences;


public class DashboardController implements Initializable {

    @FXML private Label greetingLabel, capacityLabel, savedLabel, targetLabel,
            emergencyLabel, goalsCountLabel, contribCountLabel,
            efStatusLabel, runwayLabel, sidebarCapLabel,
            pensionPctLabel, pensionSavedLabel, pensionTargetLabel;
    @FXML private ProgressBar efProgress, pensionProgress;
    @FXML private VBox alertsBox;
    @FXML private VBox goalProgressBox;           // NEW: container untuk per-goal cards
    @FXML private Label goalProgressEmptyLabel;   // NEW: label saat tidak ada goal terpilih
    @FXML private PieChart allocationChart;
    @FXML private TableView<Goal> recentGoalsTable;
    @FXML private TableColumn<Goal, String> colGoalName, colGoalCat, colGoalStorage;
    @FXML private TableColumn<Goal, Number> colGoalTarget, colGoalSaved, colGoalPct;

    private static final String PREF_KEY = "dashboard.selectedGoalIds";
    private final Set<String> selectedGoalIds = new LinkedHashSet<>();
    private boolean selectionLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        wireTable();
        loadSelectionFromPrefs();
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
                if (obj.has("interestRate")) {
                    double r = obj.get("interestRate").getAsDouble();
                    // Auto-correct data korup dari versi lama (parser membuang titik desimal).
                    // Backend sudah @Max(100), jadi >100 pasti hasil bug lama. mis. 48.0 -> 4.8, 125 -> 12.5
                    while (r > 100) r /= 10.0;
                    g.setInterestRate(r);
                }
                if (obj.has("priority"))      g.setPriority(obj.get("priority").getAsInt());
                if (obj.has("category")) {
                    try { g.setCategory(Category.valueOf(obj.get("category").getAsString())); }
                    catch (Exception ex) { g.setCategory(Category.UMUM); }
                }
                if (obj.has("storageType") && !obj.get("storageType").isJsonNull())
                    g.setStorageType(obj.get("storageType").getAsString());
                if (obj.has("storageLocation") && !obj.get("storageLocation").isJsonNull())
                    g.setStorageLocation(obj.get("storageLocation").getAsString());
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

    // ── Selection persistence ─────────────────────────────────────────────

    private Preferences prefs() {
        String user = AppSession.getInstance().getUsername();
        if (user == null || user.isBlank()) user = "default";
        return Preferences.userRoot().node("com/zenora/dashboard/" + user.replaceAll("[^a-zA-Z0-9_-]", "_"));
    }

    private void loadSelectionFromPrefs() {
        try {
            String raw = prefs().get(PREF_KEY, "");
            selectedGoalIds.clear();
            if (!raw.isBlank()) {
                for (String id : raw.split(",")) {
                    String t = id.trim();
                    if (!t.isEmpty()) selectedGoalIds.add(t);
                }
                selectionLoaded = true;
            }
        } catch (Exception ignored) {}
    }

    private void saveSelectionToPrefs() {
        try {
            prefs().put(PREF_KEY, String.join(",", selectedGoalIds));
        } catch (Exception ignored) {}
    }

    /** Goal yang berhak tampil di pemilihan (reguler — bukan Darurat/Pensiun). */
    private List<Goal> selectableGoals() {
        List<Goal> out = new ArrayList<>();
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getCategory() != null) {
                String n = g.getCategory().name();
                if (n.equals("DARURAT") || n.equals("PENSIUN")) continue;
            }
            out.add(g);
        }
        return out;
    }

    /** Goal yang sedang aktif ditampilkan di panel Progress. */
    private List<Goal> visibleGoals() {
        List<Goal> selectable = selectableGoals();
        // Default: kalau user belum pernah pilih, tampilkan semuanya.
        if (!selectionLoaded || selectedGoalIds.isEmpty()) return selectable;
        List<Goal> out = new ArrayList<>();
        for (Goal g : selectable) if (selectedGoalIds.contains(g.getId())) out.add(g);
        return out;
    }

    // ── UI ────────────────────────────────────────────────────────────────

    private void wireTable() {
        colGoalName.setCellValueFactory(c -> c.getValue().nameProperty());
        colGoalCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().getLabel()));
        colGoalTarget.setCellValueFactory(c -> c.getValue().targetAmountProperty());
        colGoalSaved.setCellValueFactory(c -> c.getValue().currentSavingProperty());
        colGoalPct.setCellValueFactory(c ->
                new javafx.beans.property.SimpleDoubleProperty(c.getValue().getProgressPercent()));
        if (colGoalStorage != null)
            colGoalStorage.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStorageType() + " — " + c.getValue().getStorageLocation()));
        formatCurrencyCol(colGoalTarget);
        formatCurrencyCol(colGoalSaved);
        colGoalPct.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(""); setStyle("-fx-background-color: transparent;"); return; }
                double pct = v.doubleValue();
                setText(String.format("%.1f%%", pct));
                if (pct >= 100) setStyle("-fx-background-color: transparent; -fx-text-fill: #34D399; -fx-font-weight: 700;");
                else if (pct >= 50) setStyle("-fx-background-color: transparent; -fx-text-fill: #FBBF24;");
                else setStyle("-fx-background-color: transparent; -fx-text-fill: #F87171;");
            }
        });
    }

    private void formatCurrencyCol(TableColumn<Goal, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color: transparent;");
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

        // ─── Stat card "TOTAL TERKUMPUL": tetap berdasarkan SEMUA goal reguler
        // (bukan Darurat/Pensiun) — biar angka totalnya tidak berubah-ubah hanya
        // karena user men-toggle pilihan di panel progress per-goal.
        double totalSaved = 0, totalTarget = 0;
        for (Goal g : selectableGoals()) {
            totalSaved  += g.getCurrentSaving();
            totalTarget += g.getTargetAmount();
        }
        savedLabel.setText(CurrencyFormatter.format(totalSaved));
        targetLabel.setText(CurrencyFormatter.format(totalTarget));

        goalsCountLabel.setText(String.valueOf(ds.getGoals().size()));
        contribCountLabel.setText(String.valueOf(ds.getContributions().size()));

        double efTarget = p.recommendedEmergencyFund();
        double efSaved = 0;
        double pensionSaved = 0, pensionTarget = 0;
        for (Goal g : ds.getGoals()) {
            if (g.getCategory() != null) {
                if (g.getCategory().name().equals("DARURAT")) efSaved += g.getCurrentSaving();
                if (g.getCategory().name().equals("PENSIUN")) {
                    pensionSaved  += g.getCurrentSaving();
                    pensionTarget += g.getTargetAmount();
                }
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

        // Pension progress
        if (pensionProgress != null) {
            double pPct = pensionTarget == 0 ? 0 : Math.min(1, pensionSaved / pensionTarget);
            pensionProgress.setProgress(pensionTarget == 0 ? 0 : pPct);
            if (pensionPctLabel != null)
                pensionPctLabel.setText(pensionTarget == 0 ? "Belum ada goal pensiun" : String.format("%.1f%%", pPct * 100));
            if (pensionSavedLabel != null)
                pensionSavedLabel.setText("Terkumpul: " + CurrencyFormatter.format(pensionSaved));
            if (pensionTargetLabel != null)
                pensionTargetLabel.setText("Target: " + (pensionTarget == 0 ? "-" : CurrencyFormatter.format(pensionTarget)));
        }

        // ─── Per-goal progress cards ─────────────────────────────────────
        renderGoalProgressCards();

        // Pie alokasi: keluarkan DARURAT & PENSIUN (kategori besar yang punya panel sendiri)
        allocationChart.getData().clear();
        for (Goal g : ds.getGoals()) {
            if (g.getTargetAmount() <= 0) continue;
            if (g.getCategory() != null) {
                String catName = g.getCategory().name();
                if (catName.equals("DARURAT") || catName.equals("PENSIUN")) continue;
            }
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
        recentGoalsTable.refresh();
    }

    /** Bangun satu mini-card per goal yang dipilih, lengkap dengan progress bar. */
    private void renderGoalProgressCards() {
        if (goalProgressBox == null) return;
        goalProgressBox.getChildren().clear();

        List<Goal> visible = visibleGoals();
        if (visible.isEmpty()) {
            Label empty = new Label(selectableGoals().isEmpty()
                    ? "Belum ada goal reguler. Tambahkan goal baru untuk mulai tracking."
                    : "Tidak ada goal terpilih. Klik “⚙ Atur” untuk memilih goal yang ingin ditampilkan.");
            empty.getStyleClass().add("zn-stat-label");
            empty.setWrapText(true);
            goalProgressBox.getChildren().add(empty);
            return;
        }

        for (Goal g : visible) {
            goalProgressBox.getChildren().add(buildGoalCard(g));
        }
    }

    private VBox buildGoalCard(Goal g) {
        double pct = g.getTargetAmount() <= 0 ? 0 : Math.min(1.0, g.getCurrentSaving() / g.getTargetAmount());
        double pctRaw = g.getProgressPercent(); // bisa > 100

        VBox card = new VBox(6);
        card.getStyleClass().add("zn-sub-panel");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1;");

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(g.getName());
        name.getStyleClass().add("zn-panel-title");
        name.setStyle("-fx-font-size: 13px;");
        Label cat = new Label(g.getCategory() != null ? g.getCategory().getLabel() : "Umum");
        cat.getStyleClass().add("zn-badge");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label pctLabel = new Label(String.format("%.1f%%", pctRaw));
        pctLabel.getStyleClass().add("zn-badge");
        if (pctRaw >= 100)      pctLabel.setStyle("-fx-text-fill: #34D399; -fx-font-weight: 700;");
        else if (pctRaw >= 50)  pctLabel.setStyle("-fx-text-fill: #FBBF24;");
        else                    pctLabel.setStyle("-fx-text-fill: #F87171;");
        header.getChildren().addAll(name, cat, spacer, pctLabel);

        // Progress bar
        ProgressBar bar = new ProgressBar(pct);
        bar.setPrefHeight(10);
        bar.setMaxWidth(Double.MAX_VALUE);
        if (pct >= 0.8)      bar.getStyleClass().setAll("progress-bar", "zn-progress", "zn-progress-green");
        else if (pct >= 0.4) bar.getStyleClass().setAll("progress-bar", "zn-progress");
        else                 bar.getStyleClass().setAll("progress-bar", "zn-progress-amber");

        // Footer line: Saved / Target  •  Sisa
        double remaining = Math.max(0, g.getTargetAmount() - g.getCurrentSaving());
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label saved = new Label(CurrencyFormatter.format(g.getCurrentSaving())
                + "  /  " + CurrencyFormatter.format(g.getTargetAmount()));
        saved.getStyleClass().add("zn-stat-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label rest = new Label(remaining == 0 ? "✓ Tercapai" : "Sisa: " + CurrencyFormatter.format(remaining));
        rest.getStyleClass().add("zn-stat-label");
        footer.getChildren().addAll(saved, sp, rest);

        card.getChildren().addAll(header, bar, footer);
        return card;
    }

    /** Buka form modal untuk memilih goal yang ditampilkan di panel Progress. */
    @FXML
    private void chooseGoalsForProgress() {
        List<Goal> selectable = selectableGoals();
        if (selectable.isEmpty()) {
            showInfo("Belum Ada Goal", "Anda belum punya goal reguler (di luar Dana Darurat & Pensiun).");
            return;
        }

        boolean selectAllDefault = !selectionLoaded || selectedGoalIds.isEmpty();
        Set<String> currentSel = new LinkedHashSet<>(selectedGoalIds);

        Optional<List<String>> res = SelectGoalsController.open(
                SceneNavigator.getPrimaryStage(), selectable, currentSel, selectAllDefault);

        if (res.isPresent()) {
            selectedGoalIds.clear();
            selectedGoalIds.addAll(res.get());
            selectionLoaded = true;
            saveSelectionToPrefs();
            renderGoalProgressCards();
        }
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
    @FXML private void openDebtPlanner()   { SceneNavigator.navigateTo("/com/zenora/fxml/DebtPlanner.fxml"); }
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
