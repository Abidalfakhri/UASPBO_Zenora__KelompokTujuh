package com.zenora.service;

import com.zenora.model.Goal;

/**
 * ABSTRACTION — Interface untuk strategy rekomendasi keuangan.
 * Memungkinkan aplikasi mengganti algoritma rekomendasi tanpa mengubah controller.
 */
public interface RecommendationStrategy {

    /** Analisis goal dan kembalikan rekomendasi. */
    RecommendationEngine.Recommendation analyze(Goal goal, double monthlyCapacity);

    /** Nama strategi untuk ditampilkan ke user. */
    String getStrategyName();
}
