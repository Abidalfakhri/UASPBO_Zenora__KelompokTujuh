package com.zenora.controller;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import com.zenora.util.SceneNavigator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class ContributionController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Contribution Log"; }

    @FXML private ChoiceBox<Goal>        goalChoice;
    @FXML private DatePicker             datePicker;
    @FXML private TextField              amountField;
    @FXML private TextField              noteField;
    @FXML private TableView<Contribution> table;
    @FXML private TableColumn<Contribution, String> colDate, colGoal, colNote;
    @FXML private TableColumn<Contribution, Number> colAmount;
    @FXML private Label totalLabel, goalProgressLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        goalChoice.setItems(DataStore.getInstance().getGoals());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());

        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getDate())));
        colGoal.setCellValueFactory(c -> {
            Goal g = DataStore.getInstance().findGoal(c.getValue().getGoalId());
            return new SimpleStringProperty(g == null ? "(dihapus)" : g.getName());
        });
        colAmount.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getAmount()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
        colNote.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNote()));

        table.setItems(DataStore.getInstance().getContributions());
        table.setPlaceholder(new Label("Belum ada setoran tercatat."));

        goalChoice.valueProperty().addListener((o, a, b) -> refreshTotals());
        DataStore.getInstance().getContributions().addListener(
                (javafx.collections.ListChangeListener<Contribution>) c -> refreshTotals());
        refreshTotals();
    }

    private void refreshTotals() {
        Goal g = goalChoice.getValue();
        if (g == null) {
            totalLabel.setText("Pilih goal untuk lihat ringkasan.");
            goalProgressLabel.setText("");
            return;
        }
        DataStore.getInstance().recomputeAllProgress();
        double total  = g.getCurrentSaving();
        double target = g.getTargetAmount();
        double pct    = target == 0 ? 0 : (total / target) * 100;
        totalLabel.setText("Total terkumpul: " + CurrencyFormatter.format(total)
                + "  dari target " + CurrencyFormatter.format(target));
        goalProgressLabel.setText(String.format(
                "Progress: %.1f%%  •  Sisa: %s", pct,
                CurrencyFormatter.format(Math.max(0, target - total))));
    }

    @FXML
    private void addContribution() {
        InputValidator v = InputValidator.create();
        Goal g = v.notNull(goalChoice.getValue(), "Goal");
        double amt = v.positiveDouble(amountField.getText(), "Nominal");
        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }
        LocalDate d = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        if (d.isAfter(LocalDate.now())) {
            new Alert(Alert.AlertType.WARNING, "Tanggal setoran tidak boleh di masa depan.").showAndWait();
            return;
        }
        Contribution c = new Contribution(g.getId(), d, amt, noteField.getText().trim());
        DataStore.getInstance().getContributions().add(c);
        DataStore.getInstance().recomputeAllProgress();
        amountField.clear();
        noteField.clear();
        refreshTotals();
    }

    @FXML
    private void removeSelected() {
        Contribution sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Pilih setoran yang ingin dihapus.").showAndWait();
            return;
        }
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus setoran " + CurrencyFormatter.format(sel.getAmount())
                + " pada " + sel.getDate() + "?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        DataStore.getInstance().getContributions().remove(sel);
        DataStore.getInstance().recomputeAllProgress();
        refreshTotals();
    }

}
