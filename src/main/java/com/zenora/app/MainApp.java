package com.zenora.app;

import com.zenora.service.StorageService;
import com.zenora.util.AppLogger;
import com.zenora.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * ✅ Entry point JavaFX Application.
 *
 * Alur:
 *   1. Inisialisasi StorageService (data lokal sebagai cache)
 *   2. Buka Login.fxml — user wajib login sebelum masuk
 *   3. Setelah login berhasil (di LoginController) → navigasi ke Dashboard
 */
public class MainApp extends Application {

    private static final Logger LOG = AppLogger.get(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Zenora starting...");

        // Load local cache (tetap dipakai untuk kompatibilitas module lama)
        StorageService.init();

        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.setTitle("Zenora — Personal Financial Suite");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // ✅ Selalu mulai dari Login screen
        SceneNavigator.navigateTo("/com/zenora/fxml/Login.fxml");

        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(e -> {
            StorageService.save();
            AppSession.getInstance().clearSession();
            LOG.info("Application closed. Session cleared.");
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
