package com.zenora.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;


public class DataStore {
    private static final DataStore INSTANCE = new DataStore();

    private final ObservableList<Goal> goals = FXCollections.observableArrayList();
    private final ObservableList<Contribution> contributions = FXCollections.observableArrayList();
    private UserProfile profile = new UserProfile();

    private DataStore() {}
    public static DataStore getInstance() { return INSTANCE; }


    public void reset() {
        goals.clear();
        contributions.clear();
        profile = new UserProfile();
    }

    public ObservableList<Goal> getGoals() { return goals; }
    public ObservableList<Contribution> getContributions() { return contributions; }
    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile p) {
        this.profile = (p == null) ? new UserProfile() : p;
    }

    /** Returns capacity from profile; kept for backwards-compatibility. */
    public double getMonthlyCapacity() { return profile.effectiveCapacity(); }
    public void setMonthlyCapacity(double v) { profile.setMonthlyCapacityOverride(v); }

    public Goal findGoal(String id) {
        for (Goal g : goals) if (id != null && id.equals(g.getId())) return g;
        return null;
    }

    public List<Contribution> contributionsFor(String goalId) {
        List<Contribution> out = new ArrayList<>();
        for (Contribution c : contributions) if (goalId.equals(c.getGoalId())) out.add(c);
        return out;
    }

    /** Sum of all logged contributions for one goal (re-computes currentSaving). */
    public double totalContributed(String goalId) {
        double sum = 0;
        for (Contribution c : contributions) if (goalId.equals(c.getGoalId())) sum += c.getAmount();
        return sum;
    }

    /** Recompute currentSaving on every goal from contributions log. */
    public void recomputeAllProgress() {
        for (Goal g : goals) {
            g.setCurrentSaving(totalContributed(g.getId()));
        }
    }
}
