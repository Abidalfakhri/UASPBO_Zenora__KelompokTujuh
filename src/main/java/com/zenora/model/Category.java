package com.zenora.model;

public enum Category {
    UMUM("Umum"),
    DARURAT("Dana Darurat"),
    PENDIDIKAN("Pendidikan"),
    RUMAH("Rumah / Properti"),
    KENDARAAN("Kendaraan"),
    PERNIKAHAN("Pernikahan"),
    LIBURAN("Liburan"),
    PENSIUN("Pensiun"),
    INVESTASI("Investasi"),
    GADGET("Gadget / Elektronik"),
    HUTANG("Hutang / Pinjaman"),
    LAINNYA("Lainnya");

    private final String label;
    Category(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
