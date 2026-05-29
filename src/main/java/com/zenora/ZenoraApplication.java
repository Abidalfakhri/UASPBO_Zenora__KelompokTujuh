package com.zenora;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ZenoraApplication {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {

        // ── 1. Jalankan Spring Boot ──────────────────────────────────────
        try {
            springContext = SpringApplication.run(ZenoraApplication.class, args);

            System.out.println("[Zenora] Spring Boot started");
            System.out.println("[Zenora] H2 Console: http://localhost:8080/h2-console");

        } catch (Exception e) {
            System.err.println("[Zenora] Failed to start Spring Boot");
            e.printStackTrace();
            return;
        }

        // ── 2. Jalankan JavaFX ───────────────────────────────────────────
        try {
            Application.launch(com.zenora.app.MainApp.class, args);

        } catch (Exception e) {
            System.err.println("[Zenora] Failed to launch JavaFX");
            e.printStackTrace();
        }
    }

    /** Ambil Spring Bean dari JavaFX controller jika diperlukan. */
    public static <T> T getBean(Class<T> beanClass) {
        if (springContext == null) {
            throw new IllegalStateException("Spring Context belum tersedia.");
        }
        return springContext.getBean(beanClass);
    }

    /** Ambil seluruh Spring Context jika diperlukan. */
    public static ConfigurableApplicationContext getContext() {
        return springContext;
    }
}