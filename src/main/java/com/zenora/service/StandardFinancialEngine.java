package com.zenora.service;

/**
 * Implementasi FinancialEngine standar — rumus keuangan baku (PMT, FV, dll).
 * ABSTRACTION: user/controller cukup tahu method tersedia, tidak perlu tahu rumusnya.
 */
public class StandardFinancialEngine extends FinancialEngine {

    @Override
    public double requiredMonthlyContribution(double futureValue, double annualRatePct, int months) {
        double r = annualRatePct / 100.0 / 12.0;
        if (r == 0) return futureValue / months;
        return futureValue * r / (Math.pow(1 + r, months) - 1);
    }

    @Override
    public int monthsToReachTarget(double target, double monthlySaving, double annualRatePct) {
        double r = annualRatePct / 100.0 / 12.0;
        if (monthlySaving <= 0) return Integer.MAX_VALUE;
        if (r == 0) return (int) Math.ceil(target / monthlySaving);
        double n = Math.log(1 + (target * r) / monthlySaving) / Math.log(1 + r);
        return (int) Math.ceil(n);
    }

    @Override
    public double retirementFundNeeded(double monthlyNeedToday, double inflationPct,
                                       int yearsToRetirement, int yearsInRetirement,
                                       double postRetirementReturnPct) {
        double monthlyNeedAtRetirement = inflateAmount(monthlyNeedToday, inflationPct, yearsToRetirement);
        double r = postRetirementReturnPct / 100.0 / 12.0;
        int n = yearsInRetirement * 12;
        if (r == 0) return monthlyNeedAtRetirement * n;
        return monthlyNeedAtRetirement * (1 - Math.pow(1 + r, -n)) / r;
    }
}
