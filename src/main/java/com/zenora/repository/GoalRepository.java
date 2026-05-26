package com.zenora.repository;

import com.zenora.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, String> {

    /** Cari goal berdasarkan nama (case-insensitive). */
    List<GoalEntity> findByNameContainingIgnoreCase(String name);

    /** Cari goal berdasarkan kategori. */
    List<GoalEntity> findByCategory(String category);

    /** Urutkan goal berdasarkan priority (1 = tertinggi). */
    List<GoalEntity> findAllByOrderByPriorityAsc();

    /** Cari goal yang currentSaving masih di bawah targetAmount (belum selesai). */
    @Query("SELECT g FROM GoalEntity g WHERE g.currentSaving < g.targetAmount ORDER BY g.priority ASC")
    List<GoalEntity> findActiveGoals();

    /** Hitung total target semua goal. */
    @Query("SELECT COALESCE(SUM(g.targetAmount), 0) FROM GoalEntity g")
    Double sumAllTargetAmounts();

    /** Hitung total tabungan saat ini dari semua goal. */
    @Query("SELECT COALESCE(SUM(g.currentSaving), 0) FROM GoalEntity g")
    Double sumAllCurrentSavings();
    java.util.List<com.zenora.entity.GoalEntity> findByOwnerUsernameOrderByPriorityAsc(String ownerUsername);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(g.targetAmount), 0) FROM GoalEntity g WHERE g.ownerUsername = :owner")
    Double sumTargetAmountsByOwner(@org.springframework.data.repository.query.Param("owner") String owner);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(g.currentSaving), 0) FROM GoalEntity g WHERE g.ownerUsername = :owner")
    Double sumCurrentSavingsByOwner(@org.springframework.data.repository.query.Param("owner") String owner);

    long countByOwnerUsername(String ownerUsername);
}
