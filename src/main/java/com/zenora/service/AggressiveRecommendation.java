package com.zenora.service;

import com.zenora.model.Goal;
import java.util.List;

/**
 * Strategi agresif — fokus memangkas pengeluaran & akselerasi pencapaian target.
 * ABSTRACTION — controller tidak perlu tahu implementasi ini berbeda.
 */
public class AggressiveRecommendation implements RecommendationStrategy {

    private final StandardFinancialEngine engine = new StandardFinancialEngine();

    @Override
    public String getStrategyName() { return "Agresif"; }

    @Override
    public RecommendationEngine.Recommendation analyze(Goal goal, double monthlyCapacity) {
        double needed = engine.requiredMonthlyContribution(
                goal.getTargetAmount(), goal.getInterestRate(), goal.getMonths());
        goal.setMonthlySaving(needed);

        boolean feasible = needed <= monthlyCapacity;
        if (feasible) {
            return new RecommendationEngine.Recommendation(true,
                    "[Agresif] Target sangat feasible! Pertimbangkan mempercepat.",
                    List.of("Tambah kontribusi 20% untuk melebihi target lebih cepat.",
                            "Coba instrumen reksa dana saham untuk return lebih tinggi."));
        }

        double shortfall = needed - monthlyCapacity;
        return new RecommendationEngine.Recommendation(false,
                String.format("[Agresif] Defisit Rp %,.0f/bulan — perlu langkah drastis.", shortfall),
                List.of(
                    String.format("Kurangi pengeluaran tidak esensial min. Rp %,.0f/bulan.", shortfall),
                    "Cari penghasilan tambahan: freelance, side hustle, atau jual aset.",
                    "Review semua langganan & tagihan — potong yang tidak perlu."
                ));
    }
}
