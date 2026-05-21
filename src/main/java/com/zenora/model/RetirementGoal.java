package com.zenora.model;

import com.zenora.service.FinancialCalculator;

/**
 * Goal pensiun — kalkulasi memperhitungkan inflasi dan withdrawal stream.
 * POLYMORPHISM — implementasi Calculable dengan rumus pensiun.
 */
public class RetirementGoal extends Goal implements Calculable {

    private final double inflationPct;
    private final int yearsToRetirement;
    private final int yearsInRetirement;
    private final double postRetirementReturn;
    private final double monthlyNeedToday;

    public RetirementGoal(String name, double monthlyNeedToday, int yearsToRetirement,
                          int yearsInRetirement, double preRetirementRate,
                          double postRetirementReturn, double inflationPct) {
        super(name, 1 /* placeholder, dihitung ulang */, yearsToRetirement * 12, preRetirementRate, 1);
        this.monthlyNeedToday    = monthlyNeedToday;
        this.yearsToRetirement   = yearsToRetirement;
        this.yearsInRetirement   = yearsInRetirement;
        this.postRetirementReturn = postRetirementReturn;
        this.inflationPct        = inflationPct;
    }

    @Override
    public double calculateTotalNeeded() {
        return FinancialCalculator.retirementFundNeeded(
                monthlyNeedToday, inflationPct, yearsToRetirement,
                yearsInRetirement, postRetirementReturn);
    }

    @Override
    public double calculateMonthlyRequired() {
        double totalNeeded = calculateTotalNeeded();
        setTargetAmount(totalNeeded);
        return FinancialCalculator.requiredMonthlyContribution(
                totalNeeded, getInterestRate(), getMonths());
    }

    @Override
    public String getSummary() {
        return String.format("[Pensiun] %s — Dana dibutuhkan: Rp %,.0f dalam %d tahun",
                getName(), calculateTotalNeeded(), yearsToRetirement);
    }
}
