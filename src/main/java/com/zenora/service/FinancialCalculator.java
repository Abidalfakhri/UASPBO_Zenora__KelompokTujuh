package com.zenora.service;

import java.util.ArrayList;
import java.util.List;


public class FinancialCalculator {

    /** Future value of a present amount with monthly compounding. */
    public static double futureValue(double presentValue, double annualRatePct, int years) {
        double r = annualRatePct / 100.0 / 12.0;
        int n = years * 12;
        return presentValue * Math.pow(1 + r, n);
    }

    /** Future value adjusted by inflation (real future cost). */
    public static double inflateAmount(double presentValue, double inflationPct, int years) {
        return presentValue * Math.pow(1 + inflationPct / 100.0, years);
    }

    /**
     * Required monthly contribution (PMT) to reach a future value
     * given monthly compounding.
     */
    public static double requiredMonthlyContribution(double futureValue, double annualRatePct, int months) {
        double r = annualRatePct / 100.0 / 12.0;
        if (r == 0) return futureValue / months;
        return futureValue * r / (Math.pow(1 + r, months) - 1);
    }

    /** How many months needed to reach target with given monthly saving. */
    public static int monthsToReachTarget(double target, double monthlySaving, double annualRatePct) {
        double r = annualRatePct / 100.0 / 12.0;
        if (monthlySaving <= 0) return Integer.MAX_VALUE;
        if (r == 0) return (int) Math.ceil(target / monthlySaving);
        double n = Math.log(1 + (target * r) / monthlySaving) / Math.log(1 + r);
        return (int) Math.ceil(n);
    }

    /**
     * Retirement: estimates total fund needed at retirement.
     * monthlyNeedToday in current rupiah, will be inflated to retirement age.
     * Uses 4% safe withdrawal assumption + life expectancy years post retirement.
     */
    public static double retirementFundNeeded(double monthlyNeedToday, double inflationPct,
                                              int yearsToRetirement, int yearsInRetirement,
                                              double postRetirementReturnPct) {
        double monthlyNeedAtRetirement = inflateAmount(monthlyNeedToday, inflationPct, yearsToRetirement);
        // Present value (at retirement) of the monthly withdrawal stream during retirement years.
        double r = postRetirementReturnPct / 100.0 / 12.0;
        int n = yearsInRetirement * 12;
        if (r == 0) return monthlyNeedAtRetirement * n;
        return monthlyNeedAtRetirement * (1 - Math.pow(1 + r, -n)) / r;
    }

    /** Emergency fund: monthly expense * months coverage. */
    public static double emergencyFundNeeded(double monthlyExpense, int monthsCoverage) {
        return monthlyExpense * monthsCoverage;
    }

    /** Simulation projection month-by-month. */
    public static List<Double> projectGrowth(double monthlySaving, double annualRatePct, int months) {
        double r = annualRatePct / 100.0 / 12.0;
        List<Double> series = new ArrayList<>();
        double balance = 0;
        for (int i = 1; i <= months; i++) {
            balance = (balance + monthlySaving) * (1 + r);
            series.add(balance);
        }
        return series;
    }
}
