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

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class ProgressController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Progress Tracking"; }

    @FXML private ChoiceBox<Goal> goalChoice;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea resultArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        goalChoice.setItems(DataStore.getInstance().getGoals());
        if (!goalChoice.getItems().isEmpty()) goalChoice.getSelectionModel().selectFirst();
        goalChoice.valueProperty().addListener((o, a, b) -> update());
        update();
    }

    @FXML
    private void update() {
        Goal g = goalChoice.getValue();
        if (g == null) {
            resultArea.setText("Belum ada goal. Buat di Goal Planning dulu.");
            progressBar.setProgress(0);
            progressLabel.setText("0.0% dari target tercapai");
            return;
        }
        DataStore.getInstance().recomputeAllProgress();

        double actual  = g.getCurrentSaving();
        double target  = g.getTargetAmount();
        double percent = g.getProgressPercent();

        progressBar.setProgress(percent / 100.0);
        progressLabel.setText(String.format("%.1f%% dari target tercapai", percent));

        // BUGFIX: getMonthlySaving() may be 0 when goal was never run through
        // the allocation engine. Fall back to computing it from plan parameters.
        double monthly = g.getMonthlySaving();
        if (monthly <= 0 && target > 0 && g.getMonths() > 0) {
            monthly = FinancialCalculator.requiredMonthlyContribution(
                    target, g.getInterestRate(), g.getMonths());
        }

        // Elapsed months since goal creation
        LocalDate start   = g.getCreatedAt() == null ? LocalDate.now() : g.getCreatedAt();
        long elapsed      = Math.max(1,
                ChronoUnit.MONTHS.between(start.withDayOfMonth(1),
                        LocalDate.now().withDayOfMonth(1)) + 1);
        double expected   = monthly * elapsed;
        double diff       = actual - expected;

        int contribCount  = DataStore.getInstance().contributionsFor(g.getId()).size();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Goal              : %s%n", g.getName()));
        sb.append(String.format("Kategori          : %s%n", g.getCategory().getLabel()));
        sb.append(String.format("Target            : %s%n", CurrencyFormatter.format(target)));
        sb.append(String.format("Aktual terkumpul  : %s (%d setoran)%n",
                CurrencyFormatter.format(actual), contribCount));
        sb.append(String.format("Tabungan rencana  : %s / bulan%n",
                CurrencyFormatter.format(monthly)));
        sb.append(String.format("Seharusnya (bln %d): %s%n", elapsed,
                CurrencyFormatter.format(expected)));
        sb.append(String.format("Sisa target       : %s%n",
                CurrencyFormatter.format(Math.max(0, target - actual))));

        if (percent >= 100) {
            sb.append("\n🎉 TARGET TERCAPAI! Selamat.\n");
        } else if (diff >= 0) {
            sb.append(String.format(
                    "%n✓ ON TRACK / LEBIH CEPAT — surplus %s%n",
                    CurrencyFormatter.format(diff)));
            sb.append("Lanjutkan disiplin menabung, atau alihkan sebagian ke goal lain.");
        } else {
            sb.append(String.format(
                    "%n✗ TERTINGGAL — kurang %s%n", CurrencyFormatter.format(-diff)));
            long monthsLeft = Math.max(1, (long) g.getMonths() - elapsed);
            double catchup  = (target - actual) / monthsLeft;
            sb.append(String.format(
                    "Untuk mengejar: butuh %s/bulan selama %d bulan tersisa.",
                    CurrencyFormatter.format(catchup), monthsLeft));
        }
        resultArea.setText(sb.toString());
    }
}
