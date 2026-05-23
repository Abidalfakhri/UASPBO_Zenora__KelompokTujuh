package com.zenora.app;

import com.zenora.service.StorageService;
import com.zenora.util.AppLogger;
import com.zenora.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOG = AppLogger.get(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOG.info("Zenora starting...");

        // Load persisted state before any UI is shown
        StorageService.init();

        SceneNavigator.setPrimaryStage(primaryStage);
        primaryStage.setTitle("Zenora — Personal Financial Suite");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Route first-time users to profile setup; returning users go to Dashboard
        boolean isNewUser = StorageService.dataFile().toFile().exists() == false
                || com.zenora.model.DataStore.getInstance().getProfile()
                       .getMonthlyIncome() == 0;
        String firstScene = isNewUser
                ? "/com/zenora/fxml/Profile.fxml"
                : "/com/zenora/fxml/Dashboard.fxml";

        SceneNavigator.navigateTo(firstScene);

        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(e -> {
            StorageService.save();
            LOG.info("Application closed. Data saved.");
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }   
}
