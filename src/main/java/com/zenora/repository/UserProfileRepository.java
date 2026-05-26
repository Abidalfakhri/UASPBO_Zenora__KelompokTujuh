package com.zenora.repository;

import com.zenora.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {

    /** Ambil profil pengguna pertama (single-user application). */
    Optional<UserProfileEntity> findFirstByOrderByCreatedAtAsc();

    Optional<UserProfileEntity> findFirstByOwnerUsernameOrderByCreatedAtAsc(String ownerUsername);

    /** Cek apakah profil sudah ada (untuk inisialisasi awal). */
    boolean existsByName(String name);
}
