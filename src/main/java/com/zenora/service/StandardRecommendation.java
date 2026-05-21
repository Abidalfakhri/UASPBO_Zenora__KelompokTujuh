package com.zenora.service;

import com.zenora.model.Goal;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategi rekomendasi standar — analisis kelayakan tabungan vs kapasitas.
 * Mengimplementasi RecommendationStrategy (ABSTRACTION).
 */
public class StandardRecommendation implements RecommendationStrategy {

    private final StandardFinancialEngine engine = new StandardFinancialEngine();

    @Override
    public String getStrategyName() { return "Standar"; }

    @Override
    public RecommendationEngine.Recommendation analyze(Goal goal, double monthlyCapacity) {
        double needed = engine.requiredMonthlyContribution(
                goal.getTargetAmount(), goal.getInterestRate(), goal.getMonths());
        goal.setMonthlySaving(needed);

        boolean feasible = needed <= monthlyCapacity;
        if (feasible) {
            return new RecommendationEngine.Recommendation(true,
                    "Target REALISTIS. Tabungan bulanan masih dalam kapasitas Anda.",
                    List.of("Pertahankan disiplin menabung tiap bulan.",
                            "Pertimbangkan instrumen investasi untuk imbal hasil lebih tinggi."));
        }

        List<String> tips = new ArrayList<>();
        int newMonths = engine.monthsToReachTarget(goal.getTargetAmount(), monthlyCapacity, goal.getInterestRate());
        if (newMonths > 0 && newMonths != Integer.MAX_VALUE)
            tips.add(String.format("Perpanjang jangka waktu menjadi ~%d bulan (%.1f tahun).", newMonths, newMonths / 12.0));

        double r = goal.getInterestRate() / 100.0 / 12.0;
        int n = goal.getMonths();
        double affordable = r == 0 ? monthlyCapacity * n : monthlyCapacity * (Math.pow(1 + r, n) - 1) / r;
        tips.add(String.format("Turunkan target menjadi maks Rp %,.0f.", affordable));
        tips.add(String.format("Tingkatkan kapasitas sebesar Rp %,.0f/bulan.", needed - monthlyCapacity));

        return new RecommendationEngine.Recommendation(false,
                String.format("Target KURANG REALISTIS. Butuh Rp %,.0f/bulan, kapasitas Rp %,.0f/bulan.", needed, monthlyCapacity),
                tips);
    }
}
