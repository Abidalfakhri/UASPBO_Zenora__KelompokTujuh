package com.zenora.util;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;


public final class AppLogger {

    private static final Logger ROOT = Logger.getLogger("com.zenora");

    static {
        ROOT.setUseParentHandlers(false);
        ROOT.setLevel(Level.ALL);

        // Console: WARNING and above
        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.WARNING);
        console.setFormatter(new SimpleFormatter());
        ROOT.addHandler(console);

        // File handler
        try {
            Path logDir = Paths.get(System.getProperty("user.home"), ".zenora", "logs");
            Files.createDirectories(logDir);
            String logFile = logDir.resolve(
                    "zenora-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log"
            ).toString();
            FileHandler fh = new FileHandler(logFile, 5 * 1024 * 1024, 3, true);
            fh.setLevel(Level.ALL);
            fh.setFormatter(new SimpleFormatter());
            ROOT.addHandler(fh);
        } catch (IOException e) {
            ROOT.warning("Could not create log file handler: " + e.getMessage());
        }
    }

    private AppLogger() {}

    public static Logger get(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    /** Convenience: log at INFO level. */
    public static void info(Class<?> clazz, String msg) {
        get(clazz).info(msg);
    }

    /** Convenience: log at WARNING level. */
    public static void warn(Class<?> clazz, String msg) {
        get(clazz).warning(msg);
    }

    /** Convenience: log at SEVERE level with exception. */
    public static void error(Class<?> clazz, String msg, Throwable t) {
        get(clazz).log(Level.SEVERE, msg, t);
    }

    /** Convenience: log at SEVERE level. */
    public static void error(Class<?> clazz, String msg) {
        get(clazz).severe(msg);
    }
}
