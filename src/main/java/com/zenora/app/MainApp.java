package com.zenora.app;

import com.zenora.service.StorageService;
import com.zenora.util.AppLogger;
import com.zenora.util.SceneNavigator;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.logging.Logger;


public class MainApp extends Application {

    private static final Logger LOG = AppLogger.get(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Zenora starting...");

        // Load local cache (tetap dipakai untuk kompatibilitas module lama)
        StorageService.init();

        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.setTitle("Zenora — Aplikasi Keuangan Pribadi");

       
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.max(960, Math.min(screen.getWidth()  * 0.85, 1366));
        double h = Math.max(640, Math.min(screen.getHeight() * 0.88, 820));

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(w);
        primaryStage.setHeight(h);
        primaryStage.centerOnScreen();

        SceneNavigator.navigateTo("/com/zenora/fxml/Login.fxml");

        primaryStage.setOnCloseRequest(e -> {
            StorageService.save();
            AppSession.getInstance().clearSession();
            LOG.info("Application closed. Session cleared.");
            try {
                var ctx = com.zenora.ZenoraApplication.getContext();
                if (ctx != null && ctx.isActive()) ctx.close();
            } catch (Exception ignored) {}
            javafx.application.Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
