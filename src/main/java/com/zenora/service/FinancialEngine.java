package com.zenora.service;

import java.util.ArrayList;
import java.util.List;

/**
 * ABSTRACTION — Abstract class yang mendefinisikan kontrak kalkulasi keuangan.
 * Detail matematika disembunyikan di dalam subclass implementasi.
 * User/controller cukup tahu method apa yang tersedia, bukan cara kerjanya.
 */
public abstract class FinancialEngine {

    // === Template method — sudah ada implementasinya ===

    /** Future value dari sejumlah uang dengan compound interest bulanan. */
    public double futureValue(double presentValue, double annualRatePct, int years) {
        double r = annualRatePct / 100.0 / 12.0;
        int n = years * 12;
        if (r == 0) return presentValue;
        return presentValue * Math.pow(1 + r, n);
    }

    /** Nilai setelah inflasi. */
    public double inflateAmount(double presentValue, double inflationPct, int years) {
        return presentValue * Math.pow(1 + inflationPct / 100.0, years);
    }

    /** Proyeksi pertumbuhan dana bulan per bulan. */
    public List<Double> projectGrowth(double monthlySaving, double annualRatePct, int months) {
        double r = annualRatePct / 100.0 / 12.0;
        List<Double> series = new ArrayList<>();
        double balance = 0;
        for (int i = 1; i <= months; i++) {
            balance = (balance + monthlySaving) * (1 + r);
            series.add(balance);
        }
        return series;
    }

    /** Dana darurat — pengeluaran × bulan. */
    public double emergencyFundNeeded(double monthlyExpense, int monthsCoverage) {
        return monthlyExpense * monthsCoverage;
    }

    // === Abstract method — WAJIB diimplementasi subclass ===

    /** Hitung kontribusi bulanan untuk mencapai future value. */
    public abstract double requiredMonthlyContribution(double futureValue, double annualRatePct, int months);

    /** Hitung berapa bulan dibutuhkan untuk mencapai target. */
    public abstract int monthsToReachTarget(double target, double monthlySaving, double annualRatePct);

    /** Hitung dana yang dibutuhkan untuk pensiun. */
    public abstract double retirementFundNeeded(double monthlyNeedToday, double inflationPct,
                                                int yearsToRetirement, int yearsInRetirement,
                                                double postRetirementReturnPct);
}
