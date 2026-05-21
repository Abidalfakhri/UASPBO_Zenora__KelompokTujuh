package com.zenora.service;

import com.google.gson.*;
import com.zenora.model.*;
import com.zenora.util.AppLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Persists all DataStore state to {@code ~/.zenora/data.json}.
 * Writes a rolling backup ({@code data.bak.json}) before every save.
 * Uses type-safe ListChangeListeners and java.util.logging.
 */
public class StorageService {

    private static final Logger LOG = AppLogger.get(StorageService.class);

    private static final Path DATA_FILE = Paths.get(
            System.getProperty("user.home"), ".zenora", "data.json");

    private static final Path BACKUP_FILE = Paths.get(
            System.getProperty("user.home"), ".zenora", "data.bak.json");

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .setPrettyPrinting()
            .create();

    private static volatile boolean initialized = false;
    private static volatile boolean suppressSave = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        load();

        DataStore ds = DataStore.getInstance();

        // BUGFIX: use properly typed ListChangeListeners (not raw <Object>)
        ds.getGoals().addListener(
                (javafx.collections.ListChangeListener<Goal>) c -> save());
        ds.getContributions().addListener(
                (javafx.collections.ListChangeListener<Contribution>) c -> save());

        LOG.info("StorageService initialised — data file: " + DATA_FILE);
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public static synchronized void save() {
        if (suppressSave) return;
        try {
            Files.createDirectories(DATA_FILE.getParent());

            // Rolling backup: copy existing file before overwriting
            if (Files.exists(DATA_FILE)) {
                Files.copy(DATA_FILE, BACKUP_FILE, StandardCopyOption.REPLACE_EXISTING);
            }

            AppStateDto dto = AppStateDto.from(DataStore.getInstance());
            String json = GSON.toJson(dto);

            // Atomic write: write to temp file then move
            Path tmp = DATA_FILE.resolveSibling("data.tmp.json");
            Files.writeString(tmp, json,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, DATA_FILE, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            LOG.fine("Data saved successfully.");

        } catch (IOException e) {
            AppLogger.error(StorageService.class, "Save failed: " + e.getMessage(), e);
        }
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    public static synchronized void load() {
        if (!Files.exists(DATA_FILE)) {
            LOG.info("No data file found — starting fresh.");
            return;
        }
        try {
            suppressSave = true;
            String json = Files.readString(DATA_FILE);
            AppStateDto dto = GSON.fromJson(json, AppStateDto.class);
            if (dto == null) {
                LOG.warning("Data file is empty or malformed.");
                return;
            }
            dto.applyTo(DataStore.getInstance());
            LOG.info("Data loaded successfully from " + DATA_FILE);

        } catch (Exception e) {
            AppLogger.error(StorageService.class,
                    "Load failed — attempting backup restore: " + e.getMessage(), e);
            tryLoadBackup();
        } finally {
            suppressSave = false;
        }
    }

    /** Attempt to restore from backup after a failed primary load. */
    private static void tryLoadBackup() {
        if (!Files.exists(BACKUP_FILE)) return;
        try {
            String json = Files.readString(BACKUP_FILE);
            AppStateDto dto = GSON.fromJson(json, AppStateDto.class);
            if (dto != null) {
                dto.applyTo(DataStore.getInstance());
                LOG.warning("Restored from backup successfully.");
            }
        } catch (Exception ex) {
            AppLogger.error(StorageService.class,
                    "Backup restore also failed: " + ex.getMessage(), ex);
        }
    }

    /** Expose data file path for display in UI. */
    public static Path dataFile() { return DATA_FILE; }

    // ─────────────────────────────  DTOs  ────────────────────────────────────

    private static class AppStateDto {
        UserProfile profile;
        List<GoalDto> goals;
        List<Contribution> contributions;

        static AppStateDto from(DataStore ds) {
            AppStateDto d = new AppStateDto();
            d.profile = ds.getProfile();
            d.goals = new ArrayList<>();
            for (Goal g : ds.getGoals()) d.goals.add(GoalDto.from(g));
            d.contributions = new ArrayList<>(ds.getContributions());
            return d;
        }

        void applyTo(DataStore ds) {
            if (profile != null) ds.setProfile(profile);
            ds.getGoals().clear();
            if (goals != null) for (GoalDto gd : goals) ds.getGoals().add(gd.toGoal());
            ds.getContributions().clear();
            if (contributions != null) ds.getContributions().addAll(contributions);
            ds.recomputeAllProgress();
        }
    }

    private static class GoalDto {
        String id;
        LocalDate createdAt;
        LocalDate targetDate;
        Category category;
        String name;
        double targetAmount;
        int months;
        double interestRate;
        int priority;
        double monthlySaving;
        double currentSaving;

        static GoalDto from(Goal g) {
            GoalDto d = new GoalDto();
            d.id           = g.getId();
            d.createdAt    = g.getCreatedAt();
            d.targetDate   = g.getTargetDate();
            d.category     = g.getCategory();
            d.name         = g.getName();
            d.targetAmount = g.getTargetAmount();
            d.months       = g.getMonths();
            d.interestRate = g.getInterestRate();
            d.priority     = g.getPriority();
            d.monthlySaving = g.getMonthlySaving();
            d.currentSaving = g.getCurrentSaving();
            return d;
        }

        Goal toGoal() {
            Goal g = new Goal();
            if (id != null)        g.setId(id);
            if (createdAt != null) g.setCreatedAt(createdAt);
            g.setTargetDate(targetDate);
            if (category != null)  g.setCategory(category);
            g.setName(name == null ? "" : name);
            g.setTargetAmount(targetAmount);
            g.setMonths(months);
            g.setInterestRate(interestRate);
            g.setPriority(priority);
            g.setMonthlySaving(monthlySaving);
            g.setCurrentSaving(currentSaving);
            return g;
        }
    }

    private static class LocalDateAdapter
            implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        @Override
        public JsonElement serialize(LocalDate src, Type t, JsonSerializationContext c) {
            return src == null
                    ? JsonNull.INSTANCE
                    : new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type t, JsonDeserializationContext c) {
            if (json == null || json.isJsonNull()) return null;
            return LocalDate.parse(json.getAsString());
        }
    }
}
