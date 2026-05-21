package com.zenora.controller;

import com.zenora.model.Category;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.RecommendationEngine;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
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

public class MultiGoalController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Multi Goal"; }

    @FXML private TextField nameField, targetField, monthsField, rateField,
                            priorityField, capacityField;
    @FXML private ChoiceBox<Category> categoryChoice;
    @FXML private TableView<Goal> goalTable;
    @FXML private TextArea resultArea;

    // Table columns
    @FXML private TableColumn<Goal, String> colName, colCat, colPct;
    @FXML private TableColumn<Goal, Number> colTarget, colMonths, colPriority, colMonthly;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (categoryChoice != null) {
            categoryChoice.setItems(FXCollections.observableArrayList(Category.values()));
            categoryChoice.setValue(Category.UMUM);
        }

        // Wire table columns
        if (colName != null)     colName.setCellValueFactory(c -> c.getValue().nameProperty());
        if (colCat != null)      colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().getLabel()));
        if (colTarget != null)   { colTarget.setCellValueFactory(c -> c.getValue().targetAmountProperty()); formatCurrency(colTarget); }
        if (colMonths != null)   colMonths.setCellValueFactory(c -> c.getValue().monthsProperty());
        if (colPriority != null) colPriority.setCellValueFactory(c -> c.getValue().priorityProperty());
        if (colMonthly != null)  { colMonthly.setCellValueFactory(c -> c.getValue().monthlySavingProperty()); formatCurrency(colMonthly); }
        if (colPct != null) {
            colPct.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f%%", c.getValue().getProgressPercent())));
            colPct.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || v == null) { setText(""); setStyle(""); return; }
                    setText(v);
                    double pct = Double.parseDouble(v.replace("%", ""));
                    if (pct >= 100) setStyle("-fx-text-fill:#34D399; -fx-font-weight:700;");
                    else if (pct >= 50) setStyle("-fx-text-fill:#FBBF24;");
                    else setStyle("-fx-text-fill:#F87171;");
                }
            });
        }

        goalTable.setItems(DataStore.getInstance().getGoals());

        // Sync capacity from profile
        double cap = DataStore.getInstance().getProfile().effectiveCapacity();
        if (cap > 0 && capacityField != null) capacityField.setText(String.valueOf((long) cap));
    }

    private void formatCurrency(TableColumn<Goal, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
    }

    @FXML
    private void addGoal() {
        InputValidator v = InputValidator.create();
        String name  = nameField.getText().trim();
        double target  = v.positiveDouble(targetField.getText(),    "Target");
        int months     = v.positiveInt(monthsField.getText(),       "Jangka waktu (bulan)");
        double rate    = v.nonNegativeDouble(rateField.getText(),   "Return (%)");
        int priority   = v.intInRange(priorityField.getText(),      "Prioritas", 1, 10);

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }

        Goal g = new Goal(name.isEmpty() ? "Goal" : name, target, months, rate, priority);
        if (categoryChoice != null && categoryChoice.getValue() != null)
            g.setCategory(categoryChoice.getValue());
        g.setTargetDate(LocalDate.now().plusMonths(months));

        DataStore.getInstance().getGoals().add(g);
        nameField.clear(); targetField.clear(); monthsField.clear();
        rateField.clear(); priorityField.clear();
        if (categoryChoice != null) categoryChoice.setValue(Category.UMUM);
    }

    @FXML
    private void removeSelected() {
        Goal sel = goalTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Pilih goal yang ingin dihapus.").showAndWait();
            return;
        }
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus goal \"" + sel.getName() + "\"?\n"
                + "Riwayat setoran terkait TIDAK ikut terhapus.",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;
        DataStore.getInstance().getGoals().remove(sel);
    }

    @FXML
    private void allocate() {
        InputValidator v = InputValidator.create();
        double cap = v.positiveDouble(capacityField != null ? capacityField.getText() : "0", "Kapasitas menabung");
        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }
        if (DataStore.getInstance().getGoals().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Belum ada goal untuk dialokasikan.").showAndWait();
            return;
        }

        DataStore.getInstance().setMonthlyCapacity(cap);
        List<String> lines = RecommendationEngine.allocateMultiGoal(
                DataStore.getInstance().getGoals(), cap);

        StringBuilder sb = new StringBuilder();
        sb.append("=== ALOKASI BERDASARKAN PRIORITAS ===\n");
        sb.append(String.format("Kapasitas menabung total: %s / bulan%n%n",
                CurrencyFormatter.format(cap)));
        for (String l : lines) sb.append(l).append("\n");
        resultArea.setText(sb.toString());
        goalTable.refresh();
    }
}
