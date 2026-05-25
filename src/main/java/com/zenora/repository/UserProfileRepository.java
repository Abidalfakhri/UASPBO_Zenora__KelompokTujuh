package com.zenora.repository;

import com.zenora.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ✅ PR-2: Repository untuk UserProfileEntity.
 *
 * ✅ KETENTUAN — Repository Pattern:
 *   Akses data profil pengguna terpusat di sini.
 *   Setiap pengguna memiliki satu profil — findFirst() dipakai karena single-user app.
 *
 * ✅ JPA/Hibernate — JpaRepository menyediakan findAll, findById, save, delete.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {

    /** Ambil profil pengguna pertama (single-user application). */
    Optional<UserProfileEntity> findFirstByOrderByCreatedAtAsc();

    Optional<UserProfileEntity> findFirstByOwnerUsernameOrderByCreatedAtAsc(String ownerUsername);

    /** Cek apakah profil sudah ada (untuk inisialisasi awal). */
    boolean existsByName(String name);
}
