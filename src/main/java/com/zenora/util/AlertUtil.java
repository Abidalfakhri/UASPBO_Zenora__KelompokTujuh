package com.zenora.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;


public final class AlertUtil {

    private AlertUtil() {}

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void warn(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void error(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /** Returns true if user clicked YES/OK. */
    public static boolean confirm(String title, String message) {
        Optional<ButtonType> res = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.YES, ButtonType.NO)
                .showAndWait();
        return res.isPresent() && res.get() == ButtonType.YES;
    }

    /** Returns true if user confirmed the deletion. */
    public static boolean confirmDelete(String itemName) {
        return confirm("Konfirmasi Hapus",
                "Apakah Anda yakin ingin menghapus \"" + itemName + "\"?\nTindakan ini tidak dapat dibatalkan.");
    }
}
