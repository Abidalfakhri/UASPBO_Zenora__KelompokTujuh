package com.zenora.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralised input-validation helper.
 * Collects multiple errors so the user sees all problems at once.
 *
 * <pre>
 *   var v = InputValidator.create();
 *   double amt = v.positiveDouble(amountField.getText(), "Nominal");
 *   int months  = v.positiveInt(monthsField.getText(), "Jangka waktu");
 *   v.throwIfInvalid();                 // throws if any error was collected
 * </pre>
 */
public final class InputValidator {

    private final List<String> errors = new ArrayList<>();

    private InputValidator() {}

    public static InputValidator create() { return new InputValidator(); }

    // ── Parsers ─────────────────────────────────────────────────────────────

    /** Parse a positive (> 0) double. Strips commas and leading/trailing spaces. */
    public double positiveDouble(String raw, String fieldName) {
        try {
            double v = parseDouble(raw);
            if (v <= 0) { errors.add(fieldName + " harus lebih dari 0."); return 0; }
            return v;
        } catch (NumberFormatException e) {
            errors.add(fieldName + " harus berupa angka yang valid.");
            return 0;
        }
    }

    /** Parse a non-negative (≥ 0) double. */
    public double nonNegativeDouble(String raw, String fieldName) {
        try {
            double v = parseDouble(raw);
            if (v < 0) { errors.add(fieldName + " tidak boleh negatif."); return 0; }
            return v;
        } catch (NumberFormatException e) {
            errors.add(fieldName + " harus berupa angka yang valid.");
            return 0;
        }
    }

    /** Parse a positive (> 0) integer. */
    public int positiveInt(String raw, String fieldName) {
        try {
            int v = Integer.parseInt(raw.trim());
            if (v <= 0) { errors.add(fieldName + " harus lebih dari 0."); return 0; }
            return v;
        } catch (NumberFormatException e) {
            errors.add(fieldName + " harus berupa angka bulat yang valid.");
            return 0;
        }
    }

    /** Parse an integer within a range [min, max]. */
    public int intInRange(String raw, String fieldName, int min, int max) {
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < min || v > max) {
                errors.add(String.format("%s harus antara %d dan %d.", fieldName, min, max));
                return min;
            }
            return v;
        } catch (NumberFormatException e) {
            errors.add(fieldName + " harus berupa angka bulat yang valid.");
            return min;
        }
    }

    /** Require that a string is not blank. Returns the trimmed value. */
    public String nonBlank(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            errors.add(fieldName + " tidak boleh kosong.");
            return "";
        }
        return raw.trim();
    }

    /** Require that an object is not null. */
    public <T> T notNull(T value, String fieldName) {
        if (value == null) {
            errors.add(fieldName + " belum dipilih.");
        }
        return value;
    }

    // ── Result ───────────────────────────────────────────────────────────────

    public boolean hasErrors() { return !errors.isEmpty(); }

    /**
     * Returns a newline-joined list of all collected error messages,
     * or {@code null} if there are no errors.
     */
    public String errorMessage() {
        return errors.isEmpty() ? null : String.join("\n", errors);
    }

    /**
     * Throws {@link ValidationException} if any error was collected.
     */
    public void throwIfInvalid() {
        if (!errors.isEmpty()) throw new ValidationException(errorMessage());
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static double parseDouble(String raw) {
        return Double.parseDouble(raw.trim().replace(",", "").replace("_", ""));
    }

    // ── Nested exception ─────────────────────────────────────────────────────

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }
}
