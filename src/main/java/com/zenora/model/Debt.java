package com.zenora.model;

import javafx.beans.property.*;


public class Debt {

    private final StringProperty  name           = new SimpleStringProperty("");
    private final DoubleProperty  balance        = new SimpleDoubleProperty(0);
    private final DoubleProperty  originalBalance= new SimpleDoubleProperty(0);
    private final DoubleProperty  aprPercent     = new SimpleDoubleProperty(0);
    private final DoubleProperty  minimumPayment = new SimpleDoubleProperty(0);
    private final DoubleProperty  totalPaid      = new SimpleDoubleProperty(0);

    public Debt() {}

    public Debt(String name, double balance, double aprPercent, double minimumPayment) {
        setName(name);
        setBalance(balance);
        setOriginalBalance(balance);
        setAprPercent(aprPercent);
        setMinimumPayment(minimumPayment);
    }

    public String  getName()           { return name.get(); }
    public void    setName(String v)   { name.set(v == null ? "" : v); }
    public StringProperty nameProperty() { return name; }

    public double  getBalance()        { return balance.get(); }
    public void    setBalance(double v){ balance.set(v); }
    public DoubleProperty balanceProperty() { return balance; }

    public double  getOriginalBalance()        { return originalBalance.get(); }
    public void    setOriginalBalance(double v){ originalBalance.set(v); }
    public DoubleProperty originalBalanceProperty() { return originalBalance; }

    public double  getAprPercent()       { return aprPercent.get(); }
    public void    setAprPercent(double v){ aprPercent.set(v); }
    public DoubleProperty aprPercentProperty() { return aprPercent; }

    public double  getMinimumPayment()       { return minimumPayment.get(); }
    public void    setMinimumPayment(double v){ minimumPayment.set(v); }
    public DoubleProperty minimumPaymentProperty() { return minimumPayment; }

    public double  getTotalPaid()        { return totalPaid.get(); }
    public void    setTotalPaid(double v){ totalPaid.set(v); }
    public DoubleProperty totalPaidProperty() { return totalPaid; }

    /** Bunga bulanan dalam rupiah berdasarkan saldo & APR saat ini. */
    public double monthlyInterest() {
        return getBalance() * (getAprPercent() / 100.0) / 12.0;
    }

    /**
     * Catat pembayaran nyata. Mengurangi saldo & menambah totalPaid.
     * Return jumlah yang benar-benar dipotong (tidak boleh > saldo).
     */
    public double recordPayment(double amount) {
        if (amount <= 0) return 0;
        double pay = Math.min(amount, getBalance());
        setBalance(getBalance() - pay);
        setTotalPaid(getTotalPaid() + pay);
        return pay;
    }

    public boolean isPaidOff() { return getBalance() <= 0.01; }
}
