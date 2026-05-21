package com.zenora.controller;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.service.FinancialCalculator;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.chart.*;

import java.net.URL;
import java.time.LocalDate;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        DataStore.getInstance().recomputeAllProgress();
        goalChoice.setItems(DataStore.getInstance().getGoals());
        goalChoice.valueProperty().addListener((o, a, b) -> redrawProjection());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();

        drawCategoryPie();
        drawMonthlyBar();
        redrawProjection();
    }

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
            plan.getData().add(new XYChart.Data<>(i + 1, projected.get(i)));
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

    private void drawCategoryPie() {
        categoryChart.getData().clear();
        Map<String, Double> sums = new LinkedHashMap<>();
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getTargetAmount() <= 0) continue;
            sums.merge(g.getCategory().getLabel(), g.getTargetAmount(), Double::sum);
        }
        if (sums.isEmpty()) return;
        for (var e : sums.entrySet()) {
            categoryChart.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
        }
    }

    private void drawMonthlyBar() {
        monthlyChart.getData().clear();
        LocalDate today = LocalDate.now();
        LinkedHashMap<String, Double> buckets = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            buckets.put(d.getYear() + "-" + String.format("%02d", d.getMonthValue()), 0.0);
        }
        for (Contribution c : DataStore.getInstance().getContributions()) {
            String key = c.getDate().getYear() + "-" + String.format("%02d", c.getDate().getMonthValue());
            if (buckets.containsKey(key)) buckets.merge(key, c.getAmount(), Double::sum);
        }
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Total setoran / bulan");
        for (var e : buckets.entrySet()) s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        monthlyChart.getData().add(s);
    }

    @FXML private void openMenu() { SceneNavigator.navigateTo("/com/zenora/fxml/Dashboard.fxml"); }
}
