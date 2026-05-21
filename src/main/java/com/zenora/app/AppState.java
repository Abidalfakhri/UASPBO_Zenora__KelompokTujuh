package com.zenora.app;


public final class AppState {

    private static final AppState INSTANCE = new AppState();

    private String currentTheme = "dark"; // future: "light"
    private String currentPage  = "dashboard";

    private AppState() {}

    public static AppState getInstance() { return INSTANCE; }

    public String getCurrentTheme() { return currentTheme; }
    public void setCurrentTheme(String theme) { this.currentTheme = theme; }

    public String getCurrentPage() { return currentPage; }
    public void setCurrentPage(String page) { this.currentPage = page; }
}
