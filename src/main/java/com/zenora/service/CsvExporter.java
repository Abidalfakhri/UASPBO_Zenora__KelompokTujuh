package com.zenora.service;

import com.zenora.model.Contribution;
import com.zenora.model.DataStore;
import com.zenora.model.Goal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Export goals / contributions to CSV files in user-selected folder. */
public class CsvExporter {

    public static Path exportAll(Path folder) throws IOException {
        Files.createDirectories(folder);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path goalsFile = folder.resolve("zenora_goals_" + stamp + ".csv");
        Path contribFile = folder.resolve("zenora_contributions_" + stamp + ".csv");

        try (BufferedWriter w = Files.newBufferedWriter(goalsFile, StandardCharsets.UTF_8)) {
            w.write("id,name,category,target,months,interest_rate,priority,monthly_required,current_saved,progress_pct,created_at,target_date\n");
            for (Goal g : DataStore.getInstance().getGoals()) {
                w.write(String.join(",",
                        esc(g.getId()),
                        esc(g.getName()),
                        esc(g.getCategory().name()),
                        String.valueOf(g.getTargetAmount()),
                        String.valueOf(g.getMonths()),
                        String.valueOf(g.getInterestRate()),
                        String.valueOf(g.getPriority()),
                        String.valueOf(g.getMonthlySaving()),
                        String.valueOf(g.getCurrentSaving()),
                        String.format("%.2f", g.getProgressPercent()),
                        String.valueOf(g.getCreatedAt()),
                        String.valueOf(g.getTargetDate())
                ));
                w.newLine();
            }
        }

        try (BufferedWriter w = Files.newBufferedWriter(contribFile, StandardCharsets.UTF_8)) {
            w.write("id,goal_id,goal_name,date,amount,note\n");
            for (Contribution c : DataStore.getInstance().getContributions()) {
                Goal g = DataStore.getInstance().findGoal(c.getGoalId());
                String gn = g == null ? "" : g.getName();
                w.write(String.join(",",
                        esc(c.getId()),
                        esc(c.getGoalId()),
                        esc(gn),
                        String.valueOf(c.getDate()),
                        String.valueOf(c.getAmount()),
                        esc(c.getNote())
                ));
                w.newLine();
            }
        }
        return folder;
    }

    private static String esc(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
