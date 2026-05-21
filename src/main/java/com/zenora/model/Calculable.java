package com.zenora.model;

/**
 * Interface POLYMORPHISM — semua jenis goal wajib bisa menghitung
 * kebutuhannya sendiri sesuai karakteristik masing-masing.
 */
public interface Calculable {

    /** Hitung tabungan/investasi bulanan yang dibutuhkan. */
    double calculateMonthlyRequired();

    /** Hitung total dana yang perlu dikumpulkan. */
    double calculateTotalNeeded();

    /** Kembalikan ringkasan hasil kalkulasi dalam bentuk teks. */
    String getSummary();
}
