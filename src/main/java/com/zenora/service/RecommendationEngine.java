package com.zenora.service;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;
import com.zenora.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    public static class Recommendation {
        public final boolean feasible;
        public final String status;
        public final List<String> suggestions;
        public final List<String> warnings;
        public Recommendation(boolean feasible, String status,
                              List<String> suggestions, List<String> warnings) {
            this.feasible = feasible;
            this.status = status;
            this.suggestions = suggestions;
            this.warnings = warnings;
        }

        /** Convenience constructor — no warnings. */
        public Recommendation(boolean feasible, String status, List<String> suggestions) {
            this(feasible, status, suggestions, new java.util.ArrayList<>());
        }
    }

    /**
     * Smart analysis: considers user profile, existing goal load, and emergency fund.
     */
    public static Recommendation analyze(Goal goal, double monthlyCapacity) {
        double needed = FinancialCalculator.requiredMonthlyContribution(
                goal.getTargetAmount(), goal.getInterestRate(), goal.getMonths());
        goal.setMonthlySaving(needed);

        // Already-allocated for other active goals
        double allocatedElsewhere = 0;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g == goal || g.getId().equals(goal.getId())) continue;
            if (g.getProgressPercent() >= 100) continue;
            allocatedElsewhere += g.getMonthlySaving();
        }
        double effectiveCapacity = Math.max(0, monthlyCapacity - allocatedElsewhere);

        List<String> tips = new ArrayList<>();
        List<String> warns = new ArrayList<>();

        // Emergency-fund awareness
        UserProfile p = DataStore.getInstance().getProfile();
        double ef = p.recommendedEmergencyFund();
        if (ef > 0) {
            double efSaved = totalSavedInCategory("DARURAT");
            if (efSaved < ef * 0.5) {
                warns.add(String.format(
                        "Dana darurat Anda baru %.0f%% dari rekomendasi (%s). Pertimbangkan menyelesaikan dana darurat dulu sebelum goal lain.",
                        ef == 0 ? 0 : (efSaved / ef * 100),
                        com.zenora.util.CurrencyFormatter.format(ef)));
            }
        }

        if (allocatedElsewhere > 0) {
            warns.add(String.format(
                    "Goal aktif lain sudah memakai %s/bulan dari kapasitas Anda. Sisa kapasitas: %s/bulan.",
                    com.zenora.util.CurrencyFormatter.format(allocatedElsewhere),
                    com.zenora.util.CurrencyFormatter.format(effectiveCapacity)));
        }

        boolean feasible = needed <= effectiveCapacity;

        if (feasible) {
            tips.add("Pertahankan disiplin menabung — gunakan modul Contribution Log untuk mencatat setoran bulanan.");
            tips.add("Pertimbangkan auto-debit ke instrumen dengan return ≥ asumsi Anda (reksa dana, deposito, obligasi).");
            if (needed < effectiveCapacity * 0.5) {
                tips.add("Kapasitas masih longgar — Anda bisa mempercepat target atau menambah goal baru.");
            }
            return new Recommendation(true,
                    String.format("Target REALISTIS. Butuh %s/bulan dari sisa kapasitas %s/bulan.",
                            com.zenora.util.CurrencyFormatter.format(needed),
                            com.zenora.util.CurrencyFormatter.format(effectiveCapacity)),
                    tips, warns);
        }

        // Not feasible — provide actionable suggestions
        int newMonths = FinancialCalculator.monthsToReachTarget(
                goal.getTargetAmount(), effectiveCapacity, goal.getInterestRate());
        if (newMonths > 0 && newMonths != Integer.MAX_VALUE) {
            tips.add(String.format("Perpanjang jangka waktu menjadi ±%d bulan (%.1f tahun) agar pas dengan sisa kapasitas.",
                    newMonths, newMonths / 12.0));
        }

        double r = goal.getInterestRate() / 100.0 / 12.0;
        int n = goal.getMonths();
        double affordableTarget;
        if (r == 0) affordableTarget = effectiveCapacity * n;
        else affordableTarget = effectiveCapacity * (Math.pow(1 + r, n) - 1) / r;
        tips.add(String.format("Turunkan target menjadi maksimal %s untuk jangka waktu yang sama.",
                com.zenora.util.CurrencyFormatter.format(affordableTarget)));

        double extra = needed - effectiveCapacity;
        tips.add(String.format("Tingkatkan penghasilan / pangkas pengeluaran sebesar %s/bulan.",
                com.zenora.util.CurrencyFormatter.format(extra)));

        if (allocatedElsewhere > 0) {
            tips.add("Atau turunkan prioritas goal lain di modul Multi-Goal Planning agar kapasitas terbebaskan.");
        }

        return new Recommendation(false,
                String.format("Target KURANG REALISTIS. Butuh %s/bulan, sisa kapasitas hanya %s/bulan.",
                        com.zenora.util.CurrencyFormatter.format(needed),
                        com.zenora.util.CurrencyFormatter.format(effectiveCapacity)),
                tips, warns);
    }

    /** Multi-goal allocation by priority (1 = highest). Distributes capacity in order. */
    public static List<String> allocateMultiGoal(List<Goal> goals, double capacity) {
        List<String> result = new ArrayList<>();
        double remaining = capacity;
        goals.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        for (Goal g : goals) {
            double need = FinancialCalculator.requiredMonthlyContribution(
                    g.getTargetAmount(), g.getInterestRate(), g.getMonths());
            g.setMonthlySaving(need);
            double allocated = Math.min(need, Math.max(0, remaining));
            remaining -= allocated;
            result.add(String.format("• [P%d] %s — butuh %s, dialokasikan %s %s",
                    g.getPriority(), g.getName(),
                    com.zenora.util.CurrencyFormatter.format(need),
                    com.zenora.util.CurrencyFormatter.format(allocated),
                    (allocated < need ? "(KURANG, prioritaskan ulang / sesuaikan)" : "✓")));
        }
        result.add("");
        result.add(String.format("Sisa kapasitas tidak terpakai: %s/bulan",
                com.zenora.util.CurrencyFormatter.format(Math.max(0, remaining))));
        return result;
    }

    private static double totalSavedInCategory(String catName) {
        double sum = 0;
        for (Goal g : DataStore.getInstance().getGoals()) {
            if (g.getCategory() != null && g.getCategory().name().equals(catName)) {
                sum += g.getCurrentSaving();
            }
        }
        return sum;
    }
}
