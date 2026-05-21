package com.zenora.model;

import com.zenora.service.FinancialCalculator;

/**
 * Goal untuk tabungan tujuan (beli rumah, liburan, gadget, dll).
 * POLYMORPHISM — implementasi Calculable dengan rumus PMT annuity.
 */
public class SavingsGoal extends Goal implements Calculable {

    public SavingsGoal(String name, double targetAmount, int months, double interestRate, int priority) {
        super(name, targetAmount, months, interestRate, priority);
    }

    @Override
    public double calculateMonthlyRequired() {
        return FinancialCalculator.requiredMonthlyContribution(
                getTargetAmount(), getInterestRate(), getMonths());
    }

    @Override
    public double calculateTotalNeeded() {
        return getTargetAmount();
    }

    @Override
    public String getSummary() {
        return String.format("[Tabungan] %s — Target: Rp %,.0f dalam %d bulan",
                getName(), getTargetAmount(), getMonths());
    }
}
