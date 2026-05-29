package com.zenora.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.ApiClient;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;


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

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(String.valueOf(item.getDayOfMonth()));
                    // Paksa warna teks kontras agar tidak "putih hilang" di tema gelap
                    setStyle("-fx-text-fill: #E5E7EB;");
                    if (item.isAfter(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-text-fill: #6B7E9A; -fx-opacity: 0.7;");
                    }
                }
            }
        });
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
                setStyle("-fx-background-color: transparent;");
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
        colNote.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNote()));

        table.setItems(DataStore.getInstance().getContributions());
        table.setPlaceholder(new Label("Memuat setoran dari server…"));

        goalChoice.valueProperty().addListener((o, a, b) -> refreshTotals());
        DataStore.getInstance().getContributions().addListener(
                (javafx.collections.ListChangeListener<Contribution>) c -> refreshTotals());


        reloadFromBackend();
        refreshTotals();
    }

    private void reloadFromBackend() {
        Thread t = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.get("/api/contributions");
            Platform.runLater(() -> {
                if (!resp.isSuccess()) {
                    table.setPlaceholder(new Label("Gagal memuat data dari server: " + resp.errorMessage()));
                    return;
                }
                try {
                    JsonArray arr = ApiClient.parseArray(resp.body);
                    DataStore.getInstance().getContributions().clear();
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        Contribution c = new Contribution();
                        if (obj.has("id"))     c.setId(obj.get("id").getAsString());
                        if (obj.has("goalId")) c.setGoalId(obj.get("goalId").getAsString());
                        if (obj.has("amount")) c.setAmount(obj.get("amount").getAsDouble());
                        if (obj.has("note") && !obj.get("note").isJsonNull())
                            c.setNote(obj.get("note").getAsString());
                        if (obj.has("date") && !obj.get("date").isJsonNull()) {
                            try { c.setDate(LocalDate.parse(obj.get("date").getAsString())); }
                            catch (Exception ex) { c.setDate(LocalDate.now()); }
                        }
                        DataStore.getInstance().getContributions().add(c);
                    }
                    DataStore.getInstance().recomputeAllProgress();
                    table.setPlaceholder(new Label("Belum ada setoran tercatat."));
                    refreshTotals();
                } catch (Exception e) {
                    table.setPlaceholder(new Label("Format data tidak terbaca."));
                }
            });
        });
        t.setDaemon(true);
        t.start();
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

        Thread thread = new Thread(() -> {
            ContribRequest req = new ContribRequest(g.getId(), amt, date.toString(), note);
            ApiClient.ApiResponse resp = ApiClient.post("/api/contributions", req);

            Platform.runLater(() -> {
                if (!resp.isSuccess()) {
                    new Alert(Alert.AlertType.ERROR,
                            "Setoran gagal disimpan ke server:\n" + resp.errorMessage())
                            .showAndWait();
                    return;
                }
    
                JsonObject obj = ApiClient.parseObject(resp.body);
                if (!obj.has("id") || obj.get("id").isJsonNull()) {
                    new Alert(Alert.AlertType.WARNING,
                            "Server tidak mengembalikan ID. Memuat ulang…").showAndWait();
                    reloadFromBackend();
                    return;
                }
                Contribution c = new Contribution(g.getId(), date, amt, note);
                c.setId(obj.get("id").getAsString());
                DataStore.getInstance().getContributions().add(0, c);
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

        if (sel.getId() == null || sel.getId().isBlank()) {
            new Alert(Alert.AlertType.ERROR,
                    "Setoran ini tidak punya ID backend. Memuat ulang…").showAndWait();
            reloadFromBackend();
            return;
        }

        // Optimistic remove
        int idx = DataStore.getInstance().getContributions().indexOf(sel);
        DataStore.getInstance().getContributions().remove(sel);
        DataStore.getInstance().recomputeAllProgress();
        refreshTotals();

        Thread thread = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.delete("/api/contributions/" + sel.getId());
            Platform.runLater(() -> {
                if (resp.isSuccess() || resp.status == 404) {
                    // 404 = sudah tidak ada di server → konsisten dengan UI
                    return;
                }
    
                int restoreAt = Math.max(0, Math.min(idx, DataStore.getInstance().getContributions().size()));
                DataStore.getInstance().getContributions().add(restoreAt, sel);
                DataStore.getInstance().recomputeAllProgress();
                refreshTotals();
                new Alert(Alert.AlertType.ERROR,
                        "Gagal menghapus dari server (" + resp.errorMessage()
                        + "). Setoran dikembalikan ke daftar.").showAndWait();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

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
