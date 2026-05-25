package com.zenora.app;

import java.util.Base64;

/**
 * ✅ Singleton — menyimpan sesi pengguna yang sedang login.
 *
 * Menyimpan username dan password (encoded) untuk dipakai sebagai
 * HTTP Basic Auth header pada setiap request ke backend REST API.
 *
 * OOP PILAR — ENCAPSULATION:
 *   Kredensial tidak bisa diakses langsung; hanya tersedia
 *   melalui getBasicAuthHeader() yang mengembalikan header siap pakai.
 */
public final class AppSession {

    private static final AppSession INSTANCE = new AppSession();

    private String username;
    private String encodedCredentials; // Base64("username:password")
    private String profileId; // ID profil di database (untuk PUT /api/profile/{id})

    private AppSession() {}

    public static AppSession getInstance() {
        return INSTANCE;
    }

    /** Simpan kredensial setelah login berhasil. */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.encodedCredentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    /** Hapus sesi saat logout. */
    public void clearSession() {
        this.username = null;
        this.encodedCredentials = null;
        this.profileId = null;
    }

    /** Apakah ada sesi aktif? */
    public boolean isLoggedIn() {
        return username != null && encodedCredentials != null;
    }

    /** Nama pengguna yang sedang login. */
    public String getUsername() {
        return username;
    }

    /** Header Authorization siap pakai: "Basic <base64>" */
    public String getBasicAuthHeader() {
        if (encodedCredentials == null) return "";
        return "Basic " + encodedCredentials;
    }

    /** ID profil di database (diisi setelah GET /api/profile). */
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
