package com.zenora.controller;

import com.zenora.app.AppSession;
import com.zenora.model.DataStore;
import com.zenora.service.ApiClient;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class RegisterController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label loadingLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        loadingLabel.setVisible(false);

        confirmPasswordField.setOnAction(e -> handleRegister());
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        // Validasi lokal
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }
        if (username.length() < 3) {
            showError("Username minimal 3 karakter.");
            return;
        }
        if (password.length() < 6) {
            showError("Password minimal 6 karakter.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Konfirmasi password tidak cocok.");
            return;
        }

        setLoading(true);

        Thread thread = new Thread(() -> {
            // POST /auth/register
            ApiClient.ApiResponse resp = ApiClient.postPublic("/auth/register",
                    new RegisterRequest(username, password));

            Platform.runLater(() -> {
                setLoading(false);
                if (resp.isSuccess()) {
                    // Registrasi berhasil — bersihkan data lama & simpan sesi baru
                    DataStore.getInstance().reset();
                    AppSession.getInstance().setProfileId(null);
                    AppSession.getInstance().setCredentials(username, password);

                    successLabel.setText("✓  Registrasi berhasil! Silakan lengkapi data profil Anda...");
                    successLabel.setVisible(true);

                    // Setelah register, user WAJIB mengisi data profil terlebih dahulu
                    // sebelum bisa masuk ke Dashboard.
                    Thread nav = new Thread(() -> {
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                        Platform.runLater(() ->
                            SceneNavigator.navigateTo("/com/zenora/fxml/Profile.fxml"));
                    });
                    nav.setDaemon(true);
                    nav.start();
                } else {
                    showError(resp.errorMessage());
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void goToLogin() {
        SceneNavigator.navigateTo("/com/zenora/fxml/Login.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        registerButton.setDisable(loading);
        loadingLabel.setVisible(loading);
        if (!loading) errorLabel.setVisible(false);
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class RegisterRequest {
        String username;
        String password;
        RegisterRequest(String u, String p) { this.username = u; this.password = p; }
    }
}
