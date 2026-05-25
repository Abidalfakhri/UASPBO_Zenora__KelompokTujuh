package com.zenora.security;

import com.zenora.entity.AppUser;
import com.zenora.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ✅ PR-4: Implementasi UserDetailsService untuk Spring Security.
 *
 * ✅ KETENTUAN — Security: autentikasi dan otorisasi.
 *   Spring Security memanggil loadUserByUsername() saat user login.
 *
 * ✅ OOP PILAR — POLYMORPHISM:
 *   Class ini IMPLEMENTS interface UserDetailsService dari Spring Security.
 *   Method loadUserByUsername() adalah override dari interface — polymorphism!
 *
 * ✅ OOP PILAR — ABSTRACTION:
 *   Spring Security tidak perlu tahu dari mana data user diambil.
 *   Cukup tahu interface UserDetailsService.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * ✅ OOP — POLYMORPHISM: override method dari interface UserDetailsService.
     * Dipanggil Spring Security saat user mencoba login.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User tidak ditemukan: " + username));

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(appUser.getRole())))
                .disabled(!appUser.isEnabled())
                .build();
    }
}
