package com.zenora.controller;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.ApiClient;
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
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * ContributionController — diperbarui untuk menyimpan/hapus kontribusi ke backend.
 *
 * Perubahan:
 *   - addContribution() → POST /api/contributions
 *   - removeSelected()  → DELETE /api/contributions/{id}
 *   - Jika backend tidak tersedia, fallback ke DataStore lokal
 */
public class ContributionController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Contribution Log"; }

    @FXML private ChoiceBox<Goal>         goalChoice;
    @FXML private DatePicker              datePicker;
    @FXML private TextField               amountField;
    @FXML private TextField               noteField;
    @FXML private TableView<Contribution> table;
    @FXML private TableColumn<Contribution, String> colDate, colGoal, colNote;
    @FXML private TableColumn<Contribution, Number> colAmount;
    @FXML private Label totalLabel, goalProgressLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        goalChoice.setItems(DataStore.getInstance().getGoals());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());
        com.zenora.util.MoneyTextFormatter.attach(amountField);

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
        Goal g   = v.notNull(goalChoice.getValue(), "Goal");
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

        final String note = noteField.getText().trim();
        final LocalDate date = d;
        amountField.clear();
        noteField.clear();

        // POST ke backend
        Thread thread = new Thread(() -> {
            ContribRequest req = new ContribRequest(g.getId(), amt, date.toString(), note);
            ApiClient.ApiResponse resp = ApiClient.post("/api/contributions", req);

            Platform.runLater(() -> {
                Contribution c;
                if (resp.isSuccess()) {
                    // Ambil contribution dari backend (sudah ada ID)
                    try {
                        com.google.gson.JsonObject obj = ApiClient.parseObject(resp.body);
                        String id = obj.has("id") ? obj.get("id").getAsString() : null;
                        c = new Contribution(g.getId(), date, amt, note);
                        if (id != null) c.setId(id);
                    } catch (Exception ex) {
                        c = new Contribution(g.getId(), date, amt, note);
                    }
                } else {
                    // Fallback lokal
                    c = new Contribution(g.getId(), date, amt, note);
                    System.err.println("[Contribution] Backend error: " + resp.errorMessage());
                }
                DataStore.getInstance().getContributions().add(c);
                DataStore.getInstance().recomputeAllProgress();
                refreshTotals();
            });
        });
        thread.setDaemon(true);
        thread.start();
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

        // Hapus dari DataStore lokal dulu (UI responsif)
        DataStore.getInstance().getContributions().remove(sel);
        DataStore.getInstance().recomputeAllProgress();
        refreshTotals();

        // DELETE dari backend
        if (sel.getId() != null && !sel.getId().isBlank()) {
            Thread thread = new Thread(() -> {
                ApiClient.ApiResponse resp = ApiClient.delete("/api/contributions/" + sel.getId());
                if (!resp.isSuccess()) {
                    System.err.println("[Contribution] Delete backend error: " + resp.errorMessage());
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class ContribRequest {
        String goalId;
        double amount;
        String date;
        String note;

        ContribRequest(String goalId, double amount, String date, String note) {
            this.goalId = goalId;
            this.amount = amount;
            this.date   = date;
            this.note   = note;
        }
    }
}
