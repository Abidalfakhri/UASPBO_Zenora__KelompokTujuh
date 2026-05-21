package com.zenora.controller;

import com.zenora.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


public abstract class BaseController {

    private static final String MAIN_MENU = "/com/zenora/fxml/MainMenu.fxml";


    @FXML
    protected void back() {
        SceneNavigator.navigateTo(MAIN_MENU);
    }


    protected void showError(TextArea area, String message) {
        area.setText("⚠ " + message);
    }

    protected double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input '" + fieldName + "' tidak valid: \"" + text.trim() + "\"");
        }
    }


    protected int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input '" + fieldName + "' harus bilangan bulat: \"" + text.trim() + "\"");
        }
    }
}
