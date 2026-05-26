package com.zenora.controller;

import com.zenora.model.Debt;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.InputValidator;
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

    private final ObservableList<Debt> debts = FXCollections.observableArrayList();

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
                setText(empty || v == null ? "" : String.format("%.2f %%", v.doubleValue()));
            }
        });

        debtTable.setItems(debts);
        debts.addListener((javafx.collections.ListChangeListener<Debt>) c -> refreshSummary());
        // Refresh summary saat saldo/totalPaid berubah karena setoran
        refreshSummary();
    }

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
                setText(empty || v == null ? "" : CurrencyFormatter.format(v.doubleValue()));
            }
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML
    private void addDebt() {
        InputValidator v = InputValidator.create();
        String name  = nameField.getText() == null ? "" : nameField.getText().trim();
        double bal   = v.positiveDouble(balanceField.getText(),  "Saldo hutang");
        double apr   = v.nonNegativeDouble(aprField.getText(),   "Bunga tahunan (APR %)");
        double min   = v.nonNegativeDouble(minPaymentField.getText(), "Minimum payment");

        if (v.hasErrors()) {
            new Alert(Alert.AlertType.WARNING, v.errorMessage()).showAndWait();
            return;
        }
        if (min > bal) {
            new Alert(Alert.AlertType.WARNING,
                    "Minimum payment tidak boleh lebih besar dari saldo.").showAndWait();
            return;
        }

        debts.add(new Debt(name.isEmpty() ? "Hutang " + (debts.size() + 1) : name,
                bal, apr, min));
        nameField.clear(); balanceField.clear(); aprField.clear(); minPaymentField.clear();
        debtTable.refresh();
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
        if (res.isPresent() && res.get() == ButtonType.YES) debts.remove(sel);
    }

    @FXML
    private void clearAll() {
        if (debts.isEmpty()) return;
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus SEMUA hutang dari daftar?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            debts.clear();
            resultArea.clear();
        }
    }

    /**
     * Catat setoran pembayaran ke hutang yang dipilih.
     * Saldo berkurang, totalPaid bertambah, ringkasan & tabel refresh.
     */
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
        String msg = String.format("Setoran %s tercatat ke \"%s\".%nSisa saldo: %s%s",
                CurrencyFormatter.format(paid), sel.getName(),
                CurrencyFormatter.format(sel.getBalance()),
                sel.isPaidOff() ? "\n\n🎉 Hutang ini LUNAS!" : "");
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
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

        // Salin daftar agar simulasi tidak mengubah data asli
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
        final int MAX_MONTHS = 12 * 60; // safety: 60 tahun

        List<String> payoffOrder = new ArrayList<>();

        while (anyOutstanding(sim) && month < MAX_MONTHS) {
            month++;

            // 1. Bunga bulanan
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double interest = d.monthlyInterest();
                d.setBalance(d.getBalance() + interest);
                totalInterest += interest;
            }

            // 2. Bayar minimum dulu
            double extraPool = extra;
            for (Debt d : sim) {
                if (d.getBalance() <= 0) continue;
                double pay = Math.min(d.getMinimumPayment(), d.getBalance());
                d.setBalance(d.getBalance() - pay);
                totalPaid += pay;
                // Jika hutang ini lunas dari minimum saja, sisa minimumnya
                // ikut menambah extra pool (efek snowball).
                if (d.getBalance() <= 0.01) {
                    extraPool += d.getMinimumPayment() - pay;
                }
            }

            // 3. Alokasikan extra pool ke target sesuai strategi
            while (extraPool > 0.01) {
                Debt target = pickTarget(sim, avalanche);
                if (target == null) break;
                double pay = Math.min(extraPool, target.getBalance());
                target.setBalance(target.getBalance() - pay);
                totalPaid += pay;
                extraPool -= pay;
                if (target.getBalance() <= 0.01) {
                    // hutang lunas — minimum-nya kini ikut snowball
                    extraPool += target.getMinimumPayment();
                    payoffOrder.add(String.format("Bulan %3d : ✓ \"%s\" LUNAS",
                            month, target.getName()));
                }
            }

            // Catat juga hutang yang lunas hanya dari minimum payment
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

        // Bandingkan dengan strategi lawan (cepat-cepat)
        sb.append("\n=== PERBANDINGAN STRATEGI ===\n");
        compare(sb, debts, extra, true,  "Avalanche");
        compare(sb, debts, extra, false, "Snowball ");

        resultArea.setText(sb.toString());
    }

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
}
