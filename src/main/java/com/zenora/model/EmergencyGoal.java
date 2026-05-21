package com.zenora.model;

import com.zenora.service.FinancialCalculator;

/**
 * Goal dana darurat — rumus: pengeluaran × bulan cakupan.
 * POLYMORPHISM — implementasi Calculable paling sederhana.
 */
public class EmergencyGoal extends Goal implements Calculable {

    private final double monthlyExpense;
    private final int coverageMonths;

    public EmergencyGoal(String name, double monthlyExpense, int coverageMonths,
                         double capacity, int priority) {
        super(name,
              FinancialCalculator.emergencyFundNeeded(monthlyExpense, coverageMonths),
              capacity > 0 ? (int) Math.ceil(
                  FinancialCalculator.emergencyFundNeeded(monthlyExpense, coverageMonths) / capacity) : 1,
              0, priority);
        this.monthlyExpense  = monthlyExpense;
        this.coverageMonths  = coverageMonths;
    }

    @Override
    public double calculateTotalNeeded() {
        return FinancialCalculator.emergencyFundNeeded(monthlyExpense, coverageMonths);
    }

    @Override
    public double calculateMonthlyRequired() {
        return getMonths() > 0 ? calculateTotalNeeded() / getMonths() : calculateTotalNeeded();
    }

    @Override
    public String getSummary() {
        return String.format("[Dana Darurat] %s — Dana: Rp %,.0f (%d bulan cakupan)",
                getName(), calculateTotalNeeded(), coverageMonths);
    }
}
