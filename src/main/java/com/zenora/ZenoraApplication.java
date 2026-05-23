package com.zenora;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class ZenoraApplication {

    /** Spring context — bisa diakses oleh komponen JavaFX jika perlu. */
    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // ── 1. Jalankan Spring Boot di background thread ──────────────────
        Thread springThread = new Thread(() -> {
            springContext = SpringApplication.run(ZenoraApplication.class, args);
            System.out.println("[Zenora] ✅ Spring Boot started on http://localhost:8080");
            System.out.println("[Zenora] ✅ H2 Console: http://localhost:8080/h2-console");
        });
        springThread.setDaemon(true); // mati saat JavaFX ditutup
        springThread.start();

        // Beri waktu Spring Boot sedikit start sebelum JavaFX membuka window
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // ── 2. Jalankan JavaFX ────────────────────────────────────────────
        Application.launch(com.zenora.app.MainApp.class, args);
    }

    /** Ambil Spring Bean dari JavaFX controller jika diperlukan. */
    public static <T> T getBean(Class<T> beanClass) {
        if (springContext == null) return null;
        return springContext.getBean(beanClass);
    }
}
