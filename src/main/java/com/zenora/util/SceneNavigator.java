package com.zenora.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Centralised scene-switch helper.
 * Preserves stage size and maximised state across navigation.
 * Shows a user-facing error dialog on FXML load failure.
 */
public final class SceneNavigator {

    private static final Logger LOG = AppLogger.get(SceneNavigator.class);

    private static Stage primaryStage;

    private SceneNavigator() {}

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Load and display the given FXML path as the primary scene.
     *
     * @param fxmlPath classpath-relative path, e.g. {@code /com/zenora/fxml/Dashboard.fxml}
     */
    public static void navigateTo(String fxmlPath) {
        try {
            URL url = SceneNavigator.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("FXML not found on classpath: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            URL css = SceneNavigator.class.getResource("/com/zenora/css/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            } else {
                LOG.warning("Stylesheet not found — UI will be unstyled.");
            }

            // Preserve size / maximised state
            double width      = primaryStage.getWidth();
            double height     = primaryStage.getHeight();
            boolean maximized = primaryStage.isMaximized();

            primaryStage.setScene(scene);

            if (Double.isFinite(width)  && width  > 0) primaryStage.setWidth(width);
            if (Double.isFinite(height) && height > 0) primaryStage.setHeight(height);
            primaryStage.setMaximized(maximized);

            LOG.fine("Navigated to " + fxmlPath);

        } catch (Exception e) {
            AppLogger.error(SceneNavigator.class,
                    "Navigation failed for " + fxmlPath + ": " + e.getMessage(), e);

            // Show user-facing dialog instead of silently failing
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Halaman tidak dapat dimuat");
            alert.setContentText("Terjadi kesalahan saat memuat \"" + fxmlPath + "\".\n\n"
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "\n\nDetail tersedia di ~/.zenora/logs/");
            alert.showAndWait();
        }
    }
}
