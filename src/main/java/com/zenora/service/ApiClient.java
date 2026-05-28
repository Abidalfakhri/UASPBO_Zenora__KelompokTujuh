package com.zenora.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zenora.app.AppSession;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


public final class ApiClient {

    private static final Logger LOG = com.zenora.util.AppLogger.get(ApiClient.class);
    /**
     * Bisa di-override saat runtime:
     *   java -Dzenora.api.base=https://api.zenora.app -jar zenora.jar
     * atau via env: ZENORA_API_BASE=https://api.zenora.app
     */
    private static final String BASE_URL = resolveBaseUrl();

    private static String resolveBaseUrl() {
        String sys = System.getProperty("zenora.api.base");
        if (sys != null && !sys.isBlank()) return sys.replaceAll("/+$", "");
        String env = System.getenv("ZENORA_API_BASE");
        if (env != null && !env.isBlank()) return env.replaceAll("/+$", "");
        return "http://localhost:8080";
    }
    private static final Gson GSON = new GsonBuilder().create();

    private ApiClient() {}

    // ── Public API ─────────────────────────────────────────────────────────

    /** GET request, mengembalikan respons JSON sebagai String. */
    public static ApiResponse get(String path) {
        return request("GET", path, null, false);
    }

    /** POST /auth/** tanpa Basic Auth. */
    public static ApiResponse postPublic(String path, Object body) {
        return request("POST", path, GSON.toJson(body), true);
    }

    /** POST request dengan Basic Auth. */
    public static ApiResponse post(String path, Object body) {
        return request("POST", path, GSON.toJson(body), false);
    }

    /** PUT request dengan Basic Auth. */
    public static ApiResponse put(String path, Object body) {
        return request("PUT", path, GSON.toJson(body), false);
    }

    /** DELETE request dengan Basic Auth. */
    public static ApiResponse delete(String path) {
        return request("DELETE", path, null, false);
    }

    /** Generic request dengan body (mis. DELETE /auth/account dengan password). */
    public static ApiResponse requestWithBody(String method, String path, Object body) {
        return request(method, path, body == null ? null : GSON.toJson(body), false);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private static ApiResponse request(String method, String path,
                                        String jsonBody, boolean skipAuth) {
        try {
            java.net.URL url = URI.create(BASE_URL + path).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Tambahkan Basic Auth kecuali untuk endpoint publik
            if (!skipAuth && AppSession.getInstance().isLoggedIn()) {
                conn.setRequestProperty("Authorization",
                        AppSession.getInstance().getBasicAuthHeader());
            }

            // Kirim body jika ada
            if (jsonBody != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();
            boolean ok = status >= 200 && status < 300;

            // Baca response body
            java.io.InputStream is = ok
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String body = "";
            if (is != null) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            LOG.fine("[ApiClient] " + method + " " + path + " → " + status);
            return new ApiResponse(status, body);

        } catch (Exception e) {
            LOG.warning("[ApiClient] Error " + method + " " + path + ": " + e.getMessage());
            return new ApiResponse(-1, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // ── Helper: parse JSON ────────────────────────────────────────────────

    public static JsonObject parseObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static JsonArray parseArray(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    // ── Response wrapper ──────────────────────────────────────────────────

    public static class ApiResponse {
        public final int status;
        public final String body;

        public ApiResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }

        /** Ambil pesan error dari JSON body {"error": "..."} atau {"message": "..."} */
        public String errorMessage() {
            try {
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("error"))   return obj.get("error").getAsString();
                if (obj.has("message")) return obj.get("message").getAsString();
            } catch (Exception ignored) {}
            return "HTTP " + status;
        }
    }
}
