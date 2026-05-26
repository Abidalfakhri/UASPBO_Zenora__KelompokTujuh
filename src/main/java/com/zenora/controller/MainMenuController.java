package com.zenora.controller;

import com.zenora.app.AppSession;
import com.zenora.model.DataStore;
import com.zenora.service.StorageService;
import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;

public class MainMenuController {
    private static final String BASE = "/com/zenora/fxml/";

    @FXML private void openDashboard()    { SceneNavigator.navigateTo(BASE + "Dashboard.fxml"); }
    @FXML private void openProfile()      { SceneNavigator.navigateTo(BASE + "Profile.fxml"); }
    @FXML private void openContributions(){ SceneNavigator.navigateTo(BASE + "Contribution.fxml"); }
    @FXML private void openReports()      { SceneNavigator.navigateTo(BASE + "Reports.fxml"); }
    @FXML private void openRetirement()   { SceneNavigator.navigateTo(BASE + "Retirement.fxml"); }
    @FXML private void openGoal()         { SceneNavigator.navigateTo(BASE + "Goal.fxml"); }
    @FXML private void openEmergency()    { SceneNavigator.navigateTo(BASE + "Emergency.fxml"); }
    @FXML private void openWhatIf()       { SceneNavigator.navigateTo(BASE + "WhatIf.fxml"); }
    @FXML private void openDebtPlanner()  { SceneNavigator.navigateTo(BASE + "DebtPlanner.fxml"); }
    @FXML private void openProgress()     { SceneNavigator.navigateTo(BASE + "Progress.fxml"); }
    @FXML private void openMilestone()    { SceneNavigator.navigateTo(BASE + "Milestone.fxml"); }

    @FXML private void saveNow()          { StorageService.save(); }

    /** Logout — hapus sesi, kembali ke Login. */
    @FXML
    private void logout() {
        AppSession.getInstance().clearSession();
        DataStore.getInstance().getGoals().clear();
        DataStore.getInstance().getContributions().clear();
        SceneNavigator.navigateTo(BASE + "Login.fxml");
    }
}
