package com.zenora.controller;

import com.zenora.app.AppSession;
import com.zenora.service.ApiClient;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * ✅ Controller untuk halaman Login.
 *
 * Flow:
 *   1. User masukkan username + password
 *   2. POST /auth/login ke Spring Boot backend
 *   3. Jika berhasil → simpan sesi di AppSession → navigasi ke Dashboard
 *   4. Jika gagal → tampilkan pesan error
 *
 * OOP PILAR — ENCAPSULATION:
 *   Kredensial tidak disimpan di field public — langsung dikirim
 *   ke AppSession setelah login berhasil.
 */
public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label loadingLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        loadingLabel.setVisible(false);

        // Tekan Enter di password field → langsung login
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validasi input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        // Tampilkan loading
        setLoading(true);

        // Jalankan HTTP request di background thread agar UI tidak freeze
        Thread thread = new Thread(() -> {
            // Daftarkan kredensial sementara untuk dicoba
            AppSession.getInstance().setCredentials(username, password);

            // POST /auth/login
            ApiClient.ApiResponse resp = ApiClient.postPublic("/auth/login",
                    new LoginRequest(username, password));

            Platform.runLater(() -> {
                setLoading(false);
                if (resp.isSuccess()) {
                    // Cek apakah user sudah pernah mengisi profil.
                    // GET /api/profile → 200 = sudah, 204 = belum.
                    Thread checkProfile = new Thread(() -> {
                        ApiClient.ApiResponse profileResp = ApiClient.get("/api/profile");
                        boolean hasProfile = profileResp.isSuccess()
                                && profileResp.status == 200
                                && profileResp.body != null
                                && !profileResp.body.isBlank();
                        Platform.runLater(() -> SceneNavigator.navigateTo(
                            hasProfile ? "/com/zenora/fxml/Dashboard.fxml"
                                       : "/com/zenora/fxml/Profile.fxml"));
                    });
                    checkProfile.setDaemon(true);
                    checkProfile.start();
                } else {
                    AppSession.getInstance().clearSession();
                    showError(resp.errorMessage());
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void goToRegister() {
        SceneNavigator.navigateTo("/com/zenora/fxml/Register.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loadingLabel.setVisible(loading);
        if (!loading) errorLabel.setVisible(false);
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class LoginRequest {
        String username;
        String password;
        LoginRequest(String u, String p) { this.username = u; this.password = p; }
    }
}
