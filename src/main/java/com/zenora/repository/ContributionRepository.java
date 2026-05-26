package com.zenora.repository;

import com.zenora.entity.ContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface ContributionRepository extends JpaRepository<ContributionEntity, String> {

    /** Ambil semua kontribusi untuk satu goal tertentu. */
    List<ContributionEntity> findByGoalIdOrderByDateDesc(String goalId);

    /** Ambil kontribusi dalam rentang tanggal tertentu. */
    List<ContributionEntity> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    /** Total kontribusi untuk satu goal. */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM ContributionEntity c WHERE c.goalId = :goalId")
    Double sumAmountByGoalId(@Param("goalId") String goalId);

    /** Total semua kontribusi. */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM ContributionEntity c")
    Double sumAllAmounts();

    /** Kontribusi bulan ini. */
    @Query("SELECT c FROM ContributionEntity c WHERE MONTH(c.date) = MONTH(CURRENT_DATE) AND YEAR(c.date) = YEAR(CURRENT_DATE)")
    List<ContributionEntity> findThisMonthContributions();
    java.util.List<com.zenora.entity.ContributionEntity> findByOwnerUsernameOrderByDateDesc(String ownerUsername);

    java.util.List<com.zenora.entity.ContributionEntity> findByGoalIdAndOwnerUsernameOrderByDateDesc(String goalId, String ownerUsername);
}
