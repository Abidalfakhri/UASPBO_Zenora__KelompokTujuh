package com.zenora.controller;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReportsController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Reports"; }

    @FXML private ChoiceBox<Goal> goalChoice;
    @FXML private LineChart<Number, Number> projectionChart;
    @FXML private BarChart<String, Number> monthlyChart;
    @FXML private PieChart categoryChart;
    @FXML private NumberAxis xAxis, yAxis;
    @FXML private Label summaryLabel;

    private static final DateTimeFormatter MONTH_SHORT =
            DateTimeFormatter.ofPattern("MMM yy", new Locale("id", "ID"));
    private static final DateTimeFormatter MONTH_LONG =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        DataStore.getInstance().recomputeAllProgress();

        goalChoice.setConverter(new StringConverter<>() {
            @Override public String toString(Goal g) { return g == null ? "" : g.getName(); }
            @Override public Goal fromString(String s) { return null; }
        });
        goalChoice.setItems(DataStore.getInstance().getGoals());
        goalChoice.valueProperty().addListener((o, a, b) -> redrawProjection());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();

        if (yAxis != null) {
            yAxis.setTickLabelFormatter(new CompactRupiahConverter());
            yAxis.setForceZeroInRange(true);
        }
        if (xAxis != null) {
            xAxis.setForceZeroInRange(false);
            xAxis.setMinorTickVisible(false);
            xAxis.setTickUnit(1);
        }
        if (projectionChart != null) {
            projectionChart.setCreateSymbols(true);
            projectionChart.setAnimated(false);
            projectionChart.setLegendVisible(true);
        }

        drawCategoryPie();
        drawMonthlyBar();
        redrawProjection();
    }

    // ── Projection ────────────────────────────────────────────────────────

    private void redrawProjection() {
        projectionChart.getData().clear();
        Goal g = goalChoice.getValue();
        if (g == null) {
            summaryLabel.setText("Pilih goal untuk lihat proyeksi.");
            return;
        }

        double monthly = g.getMonthlySaving();
        if (monthly <= 0) {
            monthly = FinancialCalculator.requiredMonthlyContribution(
                    g.getTargetAmount(), g.getInterestRate(), g.getMonths());
        }
        final double monthlyFinal = monthly;

        List<Contribution> cs = new ArrayList<>(
                DataStore.getInstance().contributionsFor(g.getId()));
        cs.sort(Comparator.comparing(Contribution::getDate));

        YearMonth anchor;
        if (g.getCreatedAt() != null) {
            anchor = YearMonth.from(g.getCreatedAt());
        } else if (!cs.isEmpty()) {
            anchor = YearMonth.from(cs.get(0).getDate());
        } else {
            anchor = YearMonth.from(LocalDate.now());
        }
        if (!cs.isEmpty()) {
            YearMonth firstC = YearMonth.from(cs.get(0).getDate());
            if (firstC.isBefore(anchor)) anchor = firstC;
        }

        int planMonths = Math.max(1, g.getMonths());

        // ── Series PROYEKSI (rencana) ─────────────────────────────────────
        List<Double> projected = FinancialCalculator.projectGrowth(
                monthlyFinal, g.getInterestRate(), planMonths);
        XYChart.Series<Number, Number> plan = new XYChart.Series<>();
        plan.setName("Proyeksi rencana");
        for (int i = 0; i < projected.size(); i++) {
            plan.getData().add(new XYChart.Data<>(i + 1, projected.get(i)));
        }

        // ── Series AKTUAL (bucket per bulan, cumulative, carry-forward) ──
        // 1) jumlahkan setoran per bulan kalender
        TreeMap<Integer, Double> perMonth = new TreeMap<>();
        for (Contribution c : cs) {
            int idx = monthIndex(anchor, c.getDate());
            if (idx < 1) idx = 1; // safety, seharusnya tidak terjadi lagi
            perMonth.merge(idx, c.getAmount(), Double::sum);
        }

        // 2) tentukan rentang bulan untuk garis aktual: dari 1 sampai
        //    max(bulan setoran terakhir, bulan berjalan).
        int currentIdx = Math.max(1, monthIndex(anchor, LocalDate.now()));
        int lastIdx = perMonth.isEmpty() ? currentIdx
                : Math.max(currentIdx, perMonth.lastKey());
        // jangan menggambar terlalu jauh melebihi planMonths + 6 bulan
        lastIdx = Math.min(lastIdx, planMonths + 6);

        XYChart.Series<Number, Number> actual = new XYChart.Series<>();
        actual.setName("Aktual (kumulatif)");
        double cum = 0;
        double totalDeposit = 0;
        int monthsWithDeposit = 0;
        for (int m = 1; m <= lastIdx; m++) {
            Double add = perMonth.get(m);
            if (add != null) {
                cum += add;
                totalDeposit += add;
                monthsWithDeposit++;
            }
            actual.getData().add(new XYChart.Data<>(m, cum));
        }

        projectionChart.getData().addAll(plan, actual);
        attachPointTooltips(plan, anchor, "Rencana");
        attachPointTooltips(actual, anchor, "Aktual");

        // ── Ringkasan + analitik ─────────────────────────────────────────
        double target = g.getTargetAmount();
        double pct = target == 0 ? 0 : Math.min(100, (cum / target) * 100);
        double plannedAtNow = projected.isEmpty() ? 0
                : projected.get(Math.min(projected.size(), currentIdx) - 1);
        double diff = cum - plannedAtNow;
        String status = Math.abs(diff) < 1
                ? "ON TRACK"
                : (diff > 0 ? "DI ATAS rencana (+" + CurrencyFormatter.format(diff) + ")"
                            : "DI BAWAH rencana (" + CurrencyFormatter.format(diff) + ")");

        int elapsed = Math.max(1, currentIdx);
        double avgPerMonth = totalDeposit / elapsed;
        double consistency = elapsed == 0 ? 0 : (monthsWithDeposit * 100.0 / elapsed);
        double remaining = Math.max(0, target - cum);
        String eta;
        if (remaining <= 0) {
            eta = "tercapai";
        } else if (avgPerMonth <= 0) {
            eta = "belum dapat diestimasi (belum ada setoran)";
        } else {
            int monthsLeft = FinancialCalculator.monthsToReachTarget(
                    remaining, avgPerMonth, g.getInterestRate());
            YearMonth done = anchor.plusMonths(currentIdx - 1L + monthsLeft);
            eta = monthsLeft + " bln lagi (~" + MONTH_LONG.format(done.atDay(1)) + ")";
        }

        summaryLabel.setText(String.format(
                "%s — target %s • terkumpul %s (%.1f%%) • butuh %s/bln%n" +
                "Status: %s  |  bulan ke-%d dari %d%n" +
                "Total setoran: %s • rata-rata %s/bln • konsistensi %.0f%% (%d/%d bln) • ETA: %s",
                g.getName(),
                CurrencyFormatter.format(target),
                CurrencyFormatter.format(cum), pct,
                CurrencyFormatter.format(monthlyFinal),
                status, currentIdx, planMonths,
                CurrencyFormatter.format(totalDeposit),
                CurrencyFormatter.format(avgPerMonth),
                consistency, monthsWithDeposit, elapsed,
                eta));
    }

    private static int monthIndex(YearMonth anchor, LocalDate date) {
        return (int) ChronoUnit.MONTHS.between(anchor, YearMonth.from(date)) + 1;
    }

    private void attachPointTooltips(XYChart.Series<Number, Number> series,
                                     YearMonth anchor, String label) {
        for (XYChart.Data<Number, Number> d : series.getData()) {
            Node n = d.getNode();
            if (n == null) continue;
            int idx = d.getXValue().intValue();
            YearMonth ym = anchor.plusMonths(idx - 1L);
            Tooltip t = new Tooltip(label + " — bulan ke-" + idx
                    + " (" + MONTH_LONG.format(ym.atDay(1)) + ")\n"
                    + CurrencyFormatter.format(d.getYValue().doubleValue()));
            Tooltip.install(n, t);
        }
    }

    // ── Category Pie ──────────────────────────────────────────────────────

    private void drawCategoryPie() {
        categoryChart.getData().clear();
        Map<String, Double> sums = new LinkedHashMap<>();
        double total = 0;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getTargetAmount() <= 0) continue;
            sums.merge(g.getCategory().getLabel(), g.getTargetAmount(), Double::sum);
            total += g.getTargetAmount();
        }
        if (sums.isEmpty()) return;

        for (var e : sums.entrySet()) {
            double pct = total == 0 ? 0 : (e.getValue() / total) * 100;
            String label = String.format("%s — %.1f%%", e.getKey(), pct);
            categoryChart.getData().add(new PieChart.Data(label, e.getValue()));
        }
        for (PieChart.Data slice : categoryChart.getData()) {
            Node n = slice.getNode();
            if (n != null) {
                Tooltip.install(n, new Tooltip(slice.getName()
                        + "\n" + CurrencyFormatter.format(slice.getPieValue())));
            }
        }
    }

    // ── Monthly Bar ───────────────────────────────────────────────────────

    private void drawMonthlyBar() {
        monthlyChart.getData().clear();
        monthlyChart.setAnimated(false);
        monthlyChart.setLegendVisible(false);
        monthlyChart.setCategoryGap(8);

        // Rentang = 11 bulan ke belakang sampai bulan setoran terbaru
        // (jadi setoran yang user catat di bulan depan tetap kelihatan).
        YearMonth now = YearMonth.now();
        YearMonth latest = now;
        for (Contribution c : DataStore.getInstance().getContributions()) {
            YearMonth ym = YearMonth.from(c.getDate());
            if (ym.isAfter(latest)) latest = ym;
        }
        YearMonth start = latest.minusMonths(11);

        LinkedHashMap<String, Double> buckets = new LinkedHashMap<>();
        for (YearMonth ym = start; !ym.isAfter(latest); ym = ym.plusMonths(1)) {
            buckets.put(MONTH_SHORT.format(ym.atDay(1)), 0.0);
        }
        for (Contribution c : DataStore.getInstance().getContributions()) {
            String key = MONTH_SHORT.format(c.getDate());
            if (buckets.containsKey(key)) buckets.merge(key, c.getAmount(), Double::sum);
        }

        if (monthlyChart.getYAxis() instanceof NumberAxis na) {
            na.setTickLabelFormatter(new CompactRupiahConverter());
            na.setForceZeroInRange(true);
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Total setoran / bulan");
        for (var e : buckets.entrySet()) {
            s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        monthlyChart.getData().add(s);

        for (XYChart.Data<String, Number> d : s.getData()) {
            Node n = d.getNode();
            if (n == null) continue;
            Tooltip.install(n, new Tooltip(d.getXValue() + "\n"
                    + CurrencyFormatter.format(d.getYValue().doubleValue())));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static class CompactRupiahConverter extends StringConverter<Number> {
        private static final DecimalFormat ONE = new DecimalFormat("0.#");
        @Override public String toString(Number n) {
            if (n == null) return "";
            double v = n.doubleValue();
            double abs = Math.abs(v);
            if (abs >= 1_000_000_000) return "Rp " + ONE.format(v / 1_000_000_000d) + " M";
            if (abs >= 1_000_000)     return "Rp " + ONE.format(v / 1_000_000d) + " jt";
            if (abs >= 1_000)         return "Rp " + ONE.format(v / 1_000d) + " rb";
            return "Rp " + ONE.format(v);
        }
        @Override public Number fromString(String s) { return 0; }
    }

    @FXML private void openMenu() { SceneNavigator.navigateTo("/com/zenora/fxml/Dashboard.fxml"); }
}
