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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Reports & Analytics — versi readable:
 *  - Sumbu Y diformat ringkas: "Rp 1,2 jt", "Rp 850 rb"
 *  - Sumbu X bar chart pakai label bulan pendek ("Mei 25") supaya tidak overlap
 *  - Pie chart menampilkan persentase di label
 *  - Tooltip pada bar & titik proyeksi (angka lengkap)
 */
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        DataStore.getInstance().recomputeAllProgress();

        // Goal selector menampilkan nama goal, bukan toString default
        goalChoice.setConverter(new StringConverter<>() {
            @Override public String toString(Goal g) { return g == null ? "" : g.getName(); }
            @Override public Goal fromString(String s) { return null; }
        });
        goalChoice.setItems(DataStore.getInstance().getGoals());
        goalChoice.valueProperty().addListener((o, a, b) -> redrawProjection());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();

        // Format sumbu Y proyeksi (Rupiah ringkas)
        if (yAxis != null) {
            yAxis.setTickLabelFormatter(new CompactRupiahConverter());
            yAxis.setForceZeroInRange(true);
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

        List<Double> projected = FinancialCalculator.projectGrowth(
                monthly, g.getInterestRate(), g.getMonths());

        XYChart.Series<Number, Number> plan = new XYChart.Series<>();
        plan.setName("Proyeksi rencana");
        for (int i = 0; i < projected.size(); i++) {
            XYChart.Data<Number, Number> d = new XYChart.Data<>(i + 1, projected.get(i));
            plan.getData().add(d);
        }

        XYChart.Series<Number, Number> actual = new XYChart.Series<>();
        actual.setName("Aktual (setoran tercatat)");
        List<Contribution> cs = DataStore.getInstance().contributionsFor(g.getId());
        cs.sort(Comparator.comparing(Contribution::getDate));
        if (!cs.isEmpty()) {
            LocalDate start = g.getCreatedAt() == null ? cs.get(0).getDate() : g.getCreatedAt();
            double cum = 0;
            for (Contribution c : cs) {
                long monthIdx = ChronoUnit.MONTHS.between(
                        start.withDayOfMonth(1), c.getDate().withDayOfMonth(1)) + 1;
                if (monthIdx < 1) monthIdx = 1;
                cum += c.getAmount();
                actual.getData().add(new XYChart.Data<>(monthIdx, cum));
            }
        }

        projectionChart.getData().addAll(plan, actual);

        // Pasang tooltip pada tiap titik (setelah node dibuat oleh JavaFX)
        attachPointTooltips(plan, "Bulan ke-");
        attachPointTooltips(actual, "Bulan ke-");

        double target  = g.getTargetAmount();
        double current = g.getCurrentSaving();
        double pct     = target == 0 ? 0 : (current / target) * 100;
        final double monthlyFinal = monthly;
        summaryLabel.setText(String.format(
                "%s  —  target %s, terkumpul %s (%.1f%%), butuh %s/bulan",
                g.getName(),
                CurrencyFormatter.format(target),
                CurrencyFormatter.format(current), pct,
                CurrencyFormatter.format(monthlyFinal)));
    }

    private void attachPointTooltips(XYChart.Series<Number, Number> series, String xPrefix) {
        for (XYChart.Data<Number, Number> d : series.getData()) {
            Node n = d.getNode();
            if (n == null) continue;
            Tooltip t = new Tooltip(xPrefix + d.getXValue() + "\n"
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
            PieChart.Data slice = new PieChart.Data(label, e.getValue());
            categoryChart.getData().add(slice);
        }

        // Tooltip lengkap (label + nominal) pada tiap slice
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

        LocalDate today = LocalDate.now();
        LinkedHashMap<String, Double> buckets = new LinkedHashMap<>();
        // gunakan label pendek "Mei 25" supaya tidak overlap
        for (int i = 11; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            buckets.put(MONTH_SHORT.format(d), 0.0);
        }
        for (Contribution c : DataStore.getInstance().getContributions()) {
            String key = MONTH_SHORT.format(c.getDate());
            if (buckets.containsKey(key)) buckets.merge(key, c.getAmount(), Double::sum);
        }

        // Format sumbu Y bar chart juga
        if (monthlyChart.getYAxis() instanceof NumberAxis na) {
            na.setTickLabelFormatter(new CompactRupiahConverter());
            na.setForceZeroInRange(true);
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Total setoran / bulan");
        for (var e : buckets.entrySet()) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(e.getKey(), e.getValue());
            s.getData().add(d);
        }
        monthlyChart.getData().add(s);

        // Tooltip pada bar
        for (XYChart.Data<String, Number> d : s.getData()) {
            Node n = d.getNode();
            if (n == null) continue;
            Tooltip t = new Tooltip(d.getXValue() + "\n"
                    + CurrencyFormatter.format(d.getYValue().doubleValue()));
            Tooltip.install(n, t);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Konverter angka → "Rp 1,2 jt" / "Rp 850 rb" supaya sumbu Y tidak penuh nol. */
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
