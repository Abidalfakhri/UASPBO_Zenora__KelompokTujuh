package com.zenora.repository;

import com.zenora.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ✅ PR-4: Repository untuk AppUser (Spring Security).
 *
 * ✅ KETENTUAN — Repository Pattern:
 *   Akses data user untuk keperluan autentikasi terpusat di sini.
 *
 * ✅ OOP — ABSTRACTION:
 *   JpaRepository menyembunyikan SQL di balik interface sederhana.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    /** Cari user berdasarkan username — dipakai oleh Spring Security. */
    Optional<AppUser> findByUsername(String username);

    /** Cek apakah username sudah terdaftar. */
    boolean existsByUsername(String username);
}
