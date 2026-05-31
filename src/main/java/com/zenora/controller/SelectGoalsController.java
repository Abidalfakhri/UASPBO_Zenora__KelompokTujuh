package com.zenora.controller;

import com.zenora.model.Goal;
import com.zenora.util.CurrencyFormatter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.*;


public class SelectGoalsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private VBox goalListBox;
    @FXML private Label countLabel;

    private final List<Goal> goals = new ArrayList<>();
    private final Map<String, CheckBox> checkboxes = new LinkedHashMap<>();
    private final Map<String, HBox> rows = new LinkedHashMap<>();

    private boolean confirmed = false;
    private List<String> result = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (searchField != null) {
            searchField.textProperty().addListener((o, a, b) -> applyFilter(b));
        }
    }

    /** Isi data goal + seleksi yang sedang aktif. */
    public void setData(List<Goal> selectable, Set<String> currentSelection, boolean selectAllDefault) {
        goals.clear();
        goals.addAll(selectable);
        goalListBox.getChildren().clear();
        checkboxes.clear();
        rows.clear();

        for (Goal g : goals) {
            boolean selected = selectAllDefault || currentSelection.contains(g.getId());
            HBox row = buildRow(g, selected);
            goalListBox.getChildren().add(row);
        }
        updateCount();
    }

    // ── Row builder ───────────────────────────────────────────────────────
    private HBox buildRow(Goal g, boolean selected) {
        HBox row = new HBox(13);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("zn-select-row");
        if (selected) row.getStyleClass().add("zn-select-row-on");

        CheckBox cb = new CheckBox();
        cb.setSelected(selected);
        cb.setFocusTraversable(false);
        checkboxes.put(g.getId(), cb);

        // Info column
        VBox info = new VBox(7);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(g.getName().isBlank() ? "(Tanpa nama)" : g.getName());
        name.getStyleClass().add("zn-select-name");
        Label cat = new Label(g.getCategory() != null ? g.getCategory().getLabel() : "Umum");
        cat.getStyleClass().add("zn-badge");
        titleRow.getChildren().addAll(name, cat);

        double pct = g.getTargetAmount() <= 0 ? 0
                : Math.min(1.0, g.getCurrentSaving() / g.getTargetAmount());
        ProgressBar bar = new ProgressBar(pct);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(7);
        bar.getStyleClass().setAll("progress-bar",
                pct >= 0.8 ? "zn-progress-green" : pct >= 0.4 ? "zn-progress" : "zn-progress-amber");

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label amounts = new Label(CurrencyFormatter.format(g.getCurrentSaving())
                + "  /  " + CurrencyFormatter.format(g.getTargetAmount()));
        amounts.getStyleClass().add("zn-select-meta");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        double pctRaw = g.getProgressPercent();
        Label pctLabel = new Label(String.format("%.0f%%", pctRaw));
        pctLabel.getStyleClass().add("zn-select-pct");
        pctLabel.getStyleClass().add(pctRaw >= 80 ? "zn-stat-green"
                : pctRaw >= 40 ? "zn-stat-amber" : "zn-stat-red");
        metaRow.getChildren().addAll(amounts, sp, pctLabel);

        info.getChildren().addAll(titleRow, bar, metaRow);
        row.getChildren().addAll(cb, info);

        // Klik di mana saja pada baris mem-toggle checkbox
        row.setOnMouseClicked(e -> {
            if (e.getTarget() != cb) cb.setSelected(!cb.isSelected());
        });
        cb.selectedProperty().addListener((o, was, now) -> {
            row.getStyleClass().remove("zn-select-row-on");
            if (now) row.getStyleClass().add("zn-select-row-on");
            updateCount();
        });

        rows.put(g.getId(), row);
        return row;
    }

    // ── Filtering ─────────────────────────────────────────────────────────
    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Goal g : goals) {
            HBox row = rows.get(g.getId());
            if (row == null) continue;
            boolean match = q.isEmpty()
                    || g.getName().toLowerCase().contains(q)
                    || (g.getCategory() != null && g.getCategory().getLabel().toLowerCase().contains(q));
            row.setVisible(match);
            row.setManaged(match);
        }
    }

    private void updateCount() {
        long c = checkboxes.values().stream().filter(CheckBox::isSelected).count();
        if (countLabel != null) countLabel.setText(c + " dipilih");
    }

    // ── Actions ───────────────────────────────────────────────────────────
    @FXML private void selectAll() {
        checkboxes.values().forEach(cb -> cb.setSelected(true));
    }

    @FXML private void clearAll() {
        checkboxes.values().forEach(cb -> cb.setSelected(false));
    }

    @FXML private void save() {
        result = new ArrayList<>();
        for (Map.Entry<String, CheckBox> e : checkboxes.entrySet()) {
            if (e.getValue().isSelected()) result.add(e.getKey());
        }
        confirmed = true;
        close();
    }

    @FXML private void cancel() {
        confirmed = false;
        close();
    }

    private void close() {
        Stage st = (Stage) goalListBox.getScene().getWindow();
        st.close();
    }

    public boolean isConfirmed() { return confirmed; }
    public List<String> getResult() { return result; }

    // ── Static helper: open modal & return selected ids (null = dibatalkan) ──
    public static Optional<List<String>> open(Window owner,
                                              List<Goal> selectable,
                                              Set<String> currentSelection,
                                              boolean selectAllDefault) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SelectGoalsController.class.getResource("/com/zenora/fxml/SelectGoals.fxml"));
            VBox root = loader.load();
            SelectGoalsController ctrl = loader.getController();
            ctrl.setData(selectable, currentSelection, selectAllDefault);

            Scene scene = new Scene(root);
            URL css = SelectGoalsController.class.getResource("/com/zenora/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Pilih Goal Dashboard");
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);
            stage.setScene(scene);
            stage.setMinWidth(480);
            stage.showAndWait();

            return ctrl.isConfirmed() ? Optional.of(ctrl.getResult()) : Optional.empty();
        } catch (Exception e) {
            System.err.println("[SelectGoals] open error: " + e.getMessage());
            return Optional.empty();
        }
    }
}
