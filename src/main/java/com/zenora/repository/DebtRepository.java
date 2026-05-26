package com.zenora.repository;

import com.zenora.entity.DebtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, String> {

    /** Ambil semua hutang milik user tertentu, diurutkan dari yang terlama dibuat. */
    List<DebtEntity> findByOwnerUsernameOrderByCreatedAtAsc(String ownerUsername);

    /** Hapus semua hutang milik user tertentu. */
    void deleteByOwnerUsername(String ownerUsername);

    /** Hitung jumlah hutang aktif (balance > 0) milik user. */
    long countByOwnerUsernameAndBalanceGreaterThan(String ownerUsername, Double balance);
}
