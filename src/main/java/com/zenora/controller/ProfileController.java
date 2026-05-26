package com.zenora.controller;

import com.google.gson.JsonObject;
import com.zenora.app.AppSession;
import com.zenora.model.DataStore;
import com.zenora.model.UserProfile;
import com.zenora.service.ApiClient;
import com.zenora.service.StorageService;
import com.zenora.util.CurrencyFormatter;
import com.zenora.util.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class ProfileController extends BaseModuleController implements Initializable {

    @Override public String moduleTitle() { return "Profile"; }
    @FXML private TextField nameField, ageField, incomeField, expenseField,
            capacityOverrideField, inflationField;
    @FXML private ChoiceBox<String> statusChoice;
    @FXML private ChoiceBox<Integer> emergencyMonthsChoice;
    @FXML private Label derivedCapacityLabel, derivedRateLabel, derivedEfLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusChoice.setItems(FXCollections.observableArrayList(
                "Single", "Menikah tanpa anak", "Menikah + anak", "Freelancer / Self-employed"));
        emergencyMonthsChoice.setItems(FXCollections.observableArrayList(3, 6, 9, 12));


        statusChoice.setValue("Single");
        emergencyMonthsChoice.setValue(6);

        com.zenora.util.MoneyTextFormatter.attach(incomeField);
        com.zenora.util.MoneyTextFormatter.attach(expenseField);
        com.zenora.util.MoneyTextFormatter.attach(capacityOverrideField);

        // Load dari backend — field diisi setelah respons diterima.
        loadProfileFromBackend();
        recompute();
        nameField.textProperty().addListener((o,a,b) -> recompute());
        incomeField.textProperty().addListener((o,a,b) -> recompute());
        expenseField.textProperty().addListener((o,a,b) -> recompute());
        capacityOverrideField.textProperty().addListener((o,a,b) -> recompute());
        emergencyMonthsChoice.valueProperty().addListener((o,a,b) -> recompute());
    }

    private void recompute() {
        double income = parse(incomeField.getText());
        double expense = parse(expenseField.getText());
        double override_ = parse(capacityOverrideField.getText());
        double capacity = override_ > 0 ? override_ : Math.max(0, income - expense);
        derivedCapacityLabel.setText(CurrencyFormatter.format(capacity) + " / bulan");
        double rate = income <= 0 ? 0 : Math.max(0, (income - expense) / income) * 100;
        derivedRateLabel.setText(String.format("%.1f%% dari pendapatan", rate));
        Integer m = emergencyMonthsChoice.getValue();
        if (m == null) m = 6;
        derivedEfLabel.setText(CurrencyFormatter.format(expense * m));
    }

    private double parse(String s) {
        try { return com.zenora.util.MoneyTextFormatter.parse(s); }
        catch (Exception e) { return 0; }
    }

    /** Parse angka desimal — toleran terhadap koma/titik (untuk inflasi, usia desimal, dll). */
    private double parseDecimal(String s) {
        if (s == null) return 0;
        String t = s.trim().replace(",", ".").replaceAll("[^0-9.\\-]", "");
        if (t.isEmpty() || t.equals("-") || t.equals(".")) return 0;
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return 0; }
    }

    @FXML
    private void save() {
        try {
            // Validasi minimum sebelum kirim ke backend
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.length() < 2) {
                new Alert(Alert.AlertType.WARNING, "Nama minimal 2 karakter.").showAndWait();
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageField.getText() == null ? "" : ageField.getText().trim());
            } catch (NumberFormatException nfe) {
                new Alert(Alert.AlertType.WARNING, "Usia harus berupa angka (17–100).").showAndWait();
                return;
            }
            if (age < 17 || age > 100) {
                new Alert(Alert.AlertType.WARNING, "Usia harus antara 17 dan 100 tahun.").showAndWait();
                return;
            }

            double inflation = parseDecimal(inflationField.getText());
            if (inflation < 0 || inflation > 50) {
                new Alert(Alert.AlertType.WARNING, "Inflasi harus antara 0% dan 50%.").showAndWait();
                return;
            }

            // Update local DataStore
            UserProfile p = DataStore.getInstance().getProfile();
            p.setName(name);
            p.setAge(age);
            p.setMonthlyIncome(parse(incomeField.getText()));
            p.setMonthlyExpense(parse(expenseField.getText()));
            p.setMonthlyCapacityOverride(parse(capacityOverrideField.getText()));
            p.setInflationPct(inflation);
            p.setHouseholdStatus(statusChoice.getValue() == null ? "Single" : statusChoice.getValue());
            p.setEmergencyMonths(emergencyMonthsChoice.getValue() == null ? 6 : emergencyMonthsChoice.getValue());
            StorageService.save();

            // Build JSON body untuk API
            ProfileRequest req = new ProfileRequest(p);

            // Simpan ke backend di background thread
            Thread thread = new Thread(() -> {
                ApiClient.ApiResponse resp;
                String profileId = AppSession.getInstance().getProfileId();

                if (profileId != null && !profileId.isBlank()) {
                    // Update profil yang sudah ada
                    resp = ApiClient.put("/api/profile/" + profileId, req);
                } else {
                    // Buat profil baru
                    resp = ApiClient.post("/api/profile", req);
                    if (resp.isSuccess()) {
                        try {
                            JsonObject obj = ApiClient.parseObject(resp.body);
                            if (obj.has("id")) {
                                AppSession.getInstance().setProfileId(obj.get("id").getAsString());
                            }
                        } catch (Exception ignored) {}
                    }
                }

                final boolean ok = resp.isSuccess();
                final String msg = ok ? "Profil tersimpan ke database." : "Tersimpan lokal. Backend: " + resp.errorMessage();

                Platform.runLater(() -> {
                    new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, msg).showAndWait();
                    if (ok) {
                        // Setelah profil tersimpan, lanjut ke Dashboard.
                        SceneNavigator.navigateTo("/com/zenora/fxml/Dashboard.fxml");
                    }
                });
            });
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Input tidak valid: " + e.getMessage()).showAndWait();
        }
    }

    /** Ambil profil milik user yang sedang login dari backend dan isi ke form. */
    private void loadProfileFromBackend() {
        Thread t = new Thread(() -> {
            ApiClient.ApiResponse resp = ApiClient.get("/api/profile");
            if (!resp.isSuccess() || resp.status == 204 || resp.body == null || resp.body.isBlank()) return;
            try {
                JsonObject obj = ApiClient.parseObject(resp.body);
                UserProfile p = DataStore.getInstance().getProfile();
                if (obj.has("id"))              AppSession.getInstance().setProfileId(obj.get("id").getAsString());
                if (obj.has("name"))            p.setName(obj.get("name").getAsString());
                if (obj.has("age"))             p.setAge(obj.get("age").getAsInt());
                if (obj.has("monthlyIncome"))   p.setMonthlyIncome(obj.get("monthlyIncome").getAsDouble());
                if (obj.has("monthlyExpense"))  p.setMonthlyExpense(obj.get("monthlyExpense").getAsDouble());
                if (obj.has("monthlyCapacityOverride")) p.setMonthlyCapacityOverride(obj.get("monthlyCapacityOverride").getAsDouble());
                if (obj.has("emergencyMonths")) p.setEmergencyMonths(obj.get("emergencyMonths").getAsInt());
                if (obj.has("householdStatus") && !obj.get("householdStatus").isJsonNull())
                                                p.setHouseholdStatus(obj.get("householdStatus").getAsString());
                if (obj.has("inflationPct"))    p.setInflationPct(obj.get("inflationPct").getAsDouble());

                Platform.runLater(() -> {
                    nameField.setText(p.getName() == null ? "" : p.getName());
                    ageField.setText(p.getAge() <= 0 ? "" : String.valueOf(p.getAge()));
                    incomeField.setText(p.getMonthlyIncome() == 0 ? "" : String.valueOf((long) p.getMonthlyIncome()));
                    expenseField.setText(p.getMonthlyExpense() == 0 ? "" : String.valueOf((long) p.getMonthlyExpense()));
                    capacityOverrideField.setText(p.getMonthlyCapacityOverride() == 0 ? "" : String.valueOf((long) p.getMonthlyCapacityOverride()));
                    inflationField.setText(String.valueOf(p.getInflationPct()));
                    statusChoice.setValue(p.getHouseholdStatus() == null ? "Single" : p.getHouseholdStatus());
                    emergencyMonthsChoice.setValue(p.getEmergencyMonths() <= 0 ? 6 : p.getEmergencyMonths());
                    recompute();
                });
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    // ── DELETE AKUN ───────────────────────────────────────────────────────

    @FXML
    private void deleteAccount() {
        // 1. Konfirmasi
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Akun dan SEMUA data (profil, goal, debt, kontribusi) akan dihapus permanen.\n\nLanjutkan?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Hapus akun " + AppSession.getInstance().getUsername() + "?");
        java.util.Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        // 2. Minta password sebagai konfirmasi
        TextInputDialog pwdDlg = new TextInputDialog();
        pwdDlg.setHeaderText("Konfirmasi password Anda");
        pwdDlg.setContentText("Password:");
        java.util.Optional<String> pwd = pwdDlg.showAndWait();
        if (pwd.isEmpty() || pwd.get().isBlank()) return;

        // 3. Kirim DELETE /auth/account dengan body { "password": "..." }
        Thread t = new Thread(() -> {
            java.util.Map<String, String> body = java.util.Map.of("password", pwd.get());
            ApiClient.ApiResponse resp = ApiClient.requestWithBody("DELETE", "/auth/account", body);
            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    AppSession.getInstance().clearSession();
                    DataStore.getInstance().reset();
                    new Alert(Alert.AlertType.INFORMATION, "Akun berhasil dihapus.").showAndWait();
                    SceneNavigator.navigateTo("/com/zenora/fxml/Login.fxml");
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Gagal menghapus akun: " + resp.errorMessage()).showAndWait();
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────

    private static class ProfileRequest {
        String name;
        int age;
        double monthlyIncome;
        double monthlyExpense;
        double monthlyCapacityOverride;
        int emergencyMonths;
        String householdStatus;
        double inflationPct;

        ProfileRequest(UserProfile p) {
            this.name                    = p.getName();
            this.age                     = p.getAge();
            this.monthlyIncome           = p.getMonthlyIncome();
            this.monthlyExpense          = p.getMonthlyExpense();
            this.monthlyCapacityOverride = p.getMonthlyCapacityOverride();
            this.emergencyMonths         = p.getEmergencyMonths();
            this.householdStatus         = p.getHouseholdStatus();
            this.inflationPct            = p.getInflationPct();
        }
    }
}
