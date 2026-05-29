package com.zenora.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zenora.model.Debt;
import com.zenora.service.ApiClient;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;


public class DebtPlannerController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Debt Payoff Planner"; }

    // ── Form input hutang ─────────────────────────────────────────────────
    @FXML private TextField nameField;
    @FXML private TextField balanceField;
    @FXML private TextField aprField;
    @FXML private TextField minPaymentField;

    // ── Strategi & dana ekstra ────────────────────────────────────────────
    @FXML private ChoiceBox<String> strategyChoice;
    @FXML private TextField extraField;

    // ── Tabel hutang ──────────────────────────────────────────────────────
    @FXML private TableView<Debt> debtTable;
    @FXML private TableColumn<Debt, String> colName;
    @FXML private TableColumn<Debt, Number> colBalance;
    @FXML private TableColumn<Debt, Number> colApr;
    @FXML private TableColumn<Debt, Number> colMin;
    @FXML private TableColumn<Debt, Number> colInterest;
    @FXML private TableColumn<Debt, Number> colPaid;
    @FXML private TableColumn<Debt, String> colStatus;

    // ── Setor pembayaran ──────────────────────────────────────────────────
    @FXML private TextField paymentField;

    // ── Ringkasan ─────────────────────────────────────────────────────────
    @FXML private Label sumTotalDebt;
    @FXML private Label sumTotalMin;
    @FXML private Label sumTotalInterest;
    @FXML private Label sumTotalPaid;
    @FXML private Label sumActiveCount;

    // ── Hasil simulasi ────────────────────────────────────────────────────
    @FXML private TextArea resultArea;

    // ObservableList untuk tabel — Debt juga menyimpan id backend-nya
    private final ObservableList<Debt> debts = FXCollections.observableArrayList();

    // Map: Debt object → backend UUID (untuk PUT/DELETE)
    private final Map<Debt, String> backendIds = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        com.zenora.util.MoneyTextFormatter.attach(balanceField);
        com.zenora.util.MoneyTextFormatter.attach(minPaymentField);
        com.zenora.util.MoneyTextFormatter.attach(extraField);
        com.zenora.util.MoneyTextFormatter.attach(paymentField);
        strategyChoice.setItems(FXCollections.observableArrayList(
                "Snowball (saldo terkecil dulu)",
                "Avalanche (bunga tertinggi dulu)"));
        strategyChoice.setValue("Avalanche (bunga tertinggi dulu)");

        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colBalance.setCellValueFactory(c -> c.getValue().balanceProperty());
        colApr.setCellValueFactory(c -> c.getValue().aprPercentProperty());
        colMin.setCellValueFactory(c -> c.getValue().minimumPaymentProperty());
        colInterest.setCellValueFactory(c ->
                new javafx.beans.property.SimpleDoubleProperty(c.getValue().monthlyInterest()));
        if (colPaid != null) colPaid.setCellValueFactory(c -> c.getValue().totalPaidProperty());
        if (colStatus != null) colStatus.setCellValueFactory(c -> {
            Debt d = c.getValue();
            double orig = d.getOriginalBalance();
            double pct  = orig <= 0 ? 0 : (d.getTotalPaid() / orig) * 100;
            String txt  = d.isPaidOff() ? "✓ LUNAS" : String.format("%.1f%% terbayar", pct);
            return new SimpleStringProperty(txt);
        });

        formatCurrency(colBalance);
        formatCurrency(colMin);
        formatCurrency(colInterest);
        if (colPaid != null) formatCurrency(colPaid);
        colApr.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color: transparent;");
                setText(empty || v == null ? "" : String.format("%.2f %%", v.doubleValue()));
            }
        });

        debtTable.setItems(debts);
        debts.addListener((javafx.collections.ListChangeListener<Debt>) c -> refreshSummary());
        refreshSummary();

        // Load data dari backend saat layar dibuka
        loadDebtsFromBackend();
    }

    // ── Load dari backend ─────────────────────────────────────────────────

    private void loadDebtsFromBackend() {
        Thread t = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.get("/api/debts");
            if (!resp.isSuccess() || resp.body == null || resp.body.isBlank()) return;
            try {
                JsonArray arr = ApiClient.parseArray(resp.body);
                List<Debt> loaded = new ArrayList<>();
                Map<Debt, String> ids = new HashMap<>();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String id      = obj.has("id")              ? obj.get("id").getAsString()              : null;
                    String name    = obj.has("name")            ? obj.get("name").getAsString()            : "";
                    double bal     = obj.has("balance")         ? obj.get("balance").getAsDouble()         : 0;
                    double orig    = obj.has("originalBalance") ? obj.get("originalBalance").getAsDouble() : bal;
                    double apr     = obj.has("aprPercent")      ? obj.get("aprPercent").getAsDouble()      : 0;
                    double minPay  = obj.has("minimumPayment")  ? obj.get("minimumPayment").getAsDouble()  : 0;
                    double paid    = obj.has("totalPaid")       ? obj.get("totalPaid").getAsDouble()       : 0;

                    Debt d = new Debt(name, orig, apr, minPay);
                    d.setBalance(bal);
                    d.setTotalPaid(paid);
                    loaded.add(d);
                    if (id != null) ids.put(d, id);
                }
                Platform.runLater(() -> {
                    debts.setAll(loaded);
                    backendIds.clear();
                    backendIds.putAll(ids);
                    debtTable.refresh();
                    refreshSummary();
                });
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Ringkasan ─────────────────────────────────────────────────────────

    private void refreshSummary() {
        if (sumTotalDebt == null) return;
        double totalDebt = 0, totalMin = 0, totalInt = 0, totalPaid = 0;
        int active = 0;
        for (Debt d : debts) {
            totalDebt += d.getBalance();
            totalMin  += d.getMinimumPayment();
            totalInt  += d.monthlyInterest();
            totalPaid += d.getTotalPaid();
            if (!d.isPaidOff()) active++;
        }
        sumTotalDebt.setText(CurrencyFormatter.format(totalDebt));
        sumTotalMin.setText(CurrencyFormatter.format(totalMin));
        sumTotalInterest.setText(CurrencyFormatter.format(totalInt));
        sumTotalPaid.setText(CurrencyFormatter.format(totalPaid));
        sumActiveCount.setText(active + " aktif / " + debts.size() + " total");
    }

    private void formatCurrency(TableColumn<Debt, Number> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                setStyle("-fx-background-color: transparent;");
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML
    private void addDebt() {
        InputValidator v = InputValidator.create();
        String name  = nameField.getText() == null ? "" : nameField.getText().trim();
        double bal   = v.positiveDouble(balanceField.getText(),      "Saldo hutang");
        double apr   = v.nonNegativeDouble(aprField.getText(),       "Bunga tahunan (APR %)");
        double min   = v.nonNegativeDouble(minPaymentField.getText(),"Minimum payment");

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }
        if (min > bal) {
            new Alert(Alert.AlertType.WARNING,
                    "Minimum payment tidak boleh lebih besar dari saldo.").showAndWait();
            return;
        }

        String debtName = name.isEmpty() ? "Hutang " + (debts.size() + 1) : name;
        Debt d = new Debt(debtName, bal, apr, min);

        // Simpan ke backend
        Thread t = new Thread(() -> {
            DebtRequest req = new DebtRequest(debtName, bal, bal, apr, min, 0.0);
            ApiClient.ApiResponse resp = ApiClient.post("/api/debts", req);
            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    try {
                        JsonObject obj = ApiClient.parseObject(resp.body);
                        if (obj.has("id")) backendIds.put(d, obj.get("id").getAsString());
                    } catch (Exception ignored) {}
                    debts.add(d);
                    nameField.clear(); balanceField.clear(); aprField.clear(); minPaymentField.clear();
                    debtTable.refresh();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Gagal menyimpan ke database: " + resp.errorMessage()).showAndWait();
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void removeSelected() {
        Debt sel = debtTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Pilih hutang yang ingin dihapus.").showAndWait();
            return;
        }
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus hutang \"" + sel.getName() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        String id = backendIds.get(sel);
        if (id != null) {
            Thread t = new Thread(() -> {
                ApiClient.ApiResponse resp = ApiClient.delete("/api/debts/" + id);
                Platform.runLater(() -> {
                    if (resp.isSuccess() || resp.status == 404) {
                        debts.remove(sel);
                        backendIds.remove(sel);
                    } else {
                        new Alert(Alert.AlertType.ERROR,
                                "Gagal menghapus dari database: " + resp.errorMessage()).showAndWait();
                    }
                });
            });
            t.setDaemon(true);
            t.start();
        } else {
            debts.remove(sel);
        }
    }

    @FXML
    private void clearAll() {
        if (debts.isEmpty()) return;
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus SEMUA hutang dari daftar?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        Thread t = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.delete("/api/debts");
            Platform.runLater(() -> {
                if (resp.isSuccess() || resp.status == 204) {
                    debts.clear();
                    backendIds.clear();
                    resultArea.clear();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Gagal menghapus dari database: " + resp.errorMessage()).showAndWait();
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

   
    @FXML
    private void payDebt() {
        Debt sel = debtTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Pilih hutang di tabel dulu, lalu masukkan nominal setoran.").showAndWait();
            return;
        }
        if (sel.isPaidOff()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Hutang \"" + sel.getName() + "\" sudah lunas.").showAndWait();
            return;
        }
        InputValidator v = InputValidator.create();
        double amount = v.positiveDouble(paymentField.getText(), "Nominal setoran");
        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }
        double paid = sel.recordPayment(amount);
        paymentField.clear();
        debtTable.refresh();
        refreshSummary();

        String msg = String.format("Setoran %s tercatat ke \"%s\".\nSisa saldo: %s%s",
                CurrencyFormatter.format(paid), sel.getName(),
                CurrencyFormatter.format(sel.getBalance()),
                sel.isPaidOff() ? "\n\n🎉 Hutang ini LUNAS!" : "");
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();

        // Sinkronkan ke backend
        String id = backendIds.get(sel);
        if (id != null) {
            final Debt finalSel = sel;
            Thread t = new Thread(() -> {
                DebtRequest req = new DebtRequest(
                        finalSel.getName(),
                        finalSel.getBalance(),
                        finalSel.getOriginalBalance(),
                        finalSel.getAprPercent(),
                        finalSel.getMinimumPayment(),
                        finalSel.getTotalPaid());
                ApiClient.put("/api/debts/" + id, req);
                // Tidak perlu alert; gagal sync tidak kritis di sini,
                // data lokal sudah tercatat.
            });
            t.setDaemon(true);
            t.start();
        }
    }

    @FXML
    private void calculate() {
        if (debts.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Belum ada hutang. Tambahkan minimal satu hutang.").showAndWait();
            return;
        }

        InputValidator v = InputValidator.create();
        double extra = v.nonNegativeDouble(
                extraField.getText() == null || extraField.getText().isBlank()
                        ? "0" : extraField.getText(),
                "Dana ekstra per bulan");
        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }

        boolean avalanche = strategyChoice.getValue() != null
                && strategyChoice.getValue().toLowerCase().startsWith("avalanche");

        List<Debt> sim = new ArrayList<>();
        double totalMin = 0;
        for (Debt d : debts) {
            sim.add(new Debt(d.getName(), d.getBalance(), d.getAprPercent(), d.getMinimumPayment()));
            totalMin += d.getMinimumPayment();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== STRATEGI: ")
                .append(avalanche ? "AVALANCHE (bunga tertinggi dulu)" : "SNOWBALL (saldo terkecil dulu)")
                .append(" ===\n");
        sb.append(String.format("Total minimum / bulan : %s%n", CurrencyFormatter.format(totalMin)));
        sb.append(String.format("Dana ekstra / bulan   : %s%n", CurrencyFormatter.format(extra)));
        sb.append(String.format("Total dana / bulan    : %s%n%n", CurrencyFormatter.format(totalMin + extra)));

        int month = 0;
        double totalInterest = 0;
        double totalPaid = 0;
        final int MAX_MONTHS = 12 * 60;

        List<String> payoffOrder = new ArrayList<>();

        while (anyOutstanding(sim) && month < MAX_MONTHS) {
            month++;
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double interest = d.monthlyInterest();
                d.setBalance(d.getBalance() + interest);
                totalInterest += interest;
            }
            double extraPool = extra;
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double pay = Math.min(d.getMinimumPayment(), d.getBalance());
                d.setBalance(d.getBalance() - pay);
                totalPaid += pay;
                if (d.getBalance() <= 0.01) extraPool += d.getMinimumPayment() - pay;
            }
            while (extraPool > 0.01) {
                Debt target = pickTarget(sim, avalanche);
                if (target == null) break;
                double pay = Math.min(extraPool, target.getBalance());
                target.setBalance(target.getBalance() - pay);
                totalPaid += pay;
                extraPool -= pay;
                if (target.getBalance() <= 0.01) {
                    extraPool += target.getMinimumPayment();
                    payoffOrder.add(String.format("Bulan %3d : ✓ \"%s\" LUNAS",
                            month, target.getName()));
                }
            }
            for (Debt d : sim) {
                if (d.getBalance() <= 0.01) {
                    String tag = String.format("Bulan %3d : ✓ \"%s\" LUNAS", month, d.getName());
                    if (!payoffOrder.contains(tag)
                            && payoffOrder.stream().noneMatch(s -> s.contains("\"" + d.getName() + "\""))) {
                        payoffOrder.add(tag);
                    }
                    d.setBalance(0);
                }
            }
        }

        if (month >= MAX_MONTHS) {
            sb.append("⚠ Setoran terlalu kecil — hutang tidak lunas dalam 60 tahun.\n");
            sb.append("   Tambahkan dana ekstra atau naikkan minimum payment.\n\n");
        } else {
            sb.append("=== HASIL ===\n");
            sb.append(String.format("Bebas hutang dalam : %d bulan (%.1f tahun)%n",
                    month, month / 12.0));
            sb.append(String.format("Total bunga dibayar: %s%n",
                    CurrencyFormatter.format(totalInterest)));
            sb.append(String.format("Total uang keluar  : %s%n%n",
                    CurrencyFormatter.format(totalPaid)));
            sb.append("=== URUTAN PELUNASAN ===\n");
            for (String s : payoffOrder) sb.append(s).append("\n");
        }

        sb.append("\n=== PERBANDINGAN STRATEGI ===\n");
        compare(sb, debts, extra, true,  "Avalanche");
        compare(sb, debts, extra, false, "Snowball ");

        resultArea.setText(sb.toString());
    }

    // ── Simulation helpers ────────────────────────────────────────────────

    private boolean anyOutstanding(List<Debt> list) {
        for (Debt d : list) if (d.getBalance() > 0.01) return true;
        return false;
    }

    private Debt pickTarget(List<Debt> list, boolean avalanche) {
        Debt best = null;
        for (Debt d : list) {
            if (d.getBalance() <= 0.01) continue;
            if (best == null) { best = d; continue; }
            if (avalanche) {
                if (d.getAprPercent() > best.getAprPercent()) best = d;
            } else {
                if (d.getBalance() < best.getBalance()) best = d;
            }
        }
        return best;
    }

    private void compare(StringBuilder sb, List<Debt> source,
                         double extra, boolean avalanche, String label) {
        List<Debt> sim = new ArrayList<>();
        for (Debt d : source) {
            sim.add(new Debt(d.getName(), d.getBalance(), d.getAprPercent(), d.getMinimumPayment()));
        }
        int month = 0;
        double totalInterest = 0;
        final int MAX_MONTHS = 12 * 60;
        while (anyOutstanding(sim) && month < MAX_MONTHS) {
            month++;
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double interest = d.monthlyInterest();
                d.setBalance(d.getBalance() + interest);
                totalInterest += interest;
            }
            double pool = extra;
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double pay = Math.min(d.getMinimumPayment(), d.getBalance());
                d.setBalance(d.getBalance() - pay);
                if (d.getBalance() <= 0.01) pool += d.getMinimumPayment() - pay;
            }
            while (pool > 0.01) {
                Debt t = pickTarget(sim, avalanche);
                if (t == null) break;
                double pay = Math.min(pool, t.getBalance());
                t.setBalance(t.getBalance() - pay);
                pool -= pay;
                if (t.getBalance() <= 0.01) pool += t.getMinimumPayment();
            }
        }
        if (month >= MAX_MONTHS) {
            sb.append(String.format("%s : tidak lunas dalam 60 tahun%n", label));
        } else {
            sb.append(String.format("%s : %d bulan, bunga %s%n",
                    label, month, CurrencyFormatter.format(totalInterest)));
        }
    }

    // ── Inner DTO (untuk serialisasi ke JSON via ApiClient) ───────────────

    private static class DebtRequest {
        String name;
        double balance;
        double originalBalance;
        double aprPercent;
        double minimumPayment;
        double totalPaid;

        DebtRequest(String name, double balance, double originalBalance,
                    double aprPercent, double minimumPayment, double totalPaid) {
            this.name            = name;
            this.balance         = balance;
            this.originalBalance = originalBalance;
            this.aprPercent      = aprPercent;
            this.minimumPayment  = minimumPayment;
            this.totalPaid       = totalPaid;
        }
    }
}
