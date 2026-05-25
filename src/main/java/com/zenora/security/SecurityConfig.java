package com.zenora.security;

import com.zenora.entity.AppUser;
import com.zenora.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ✅ PR-4: Konfigurasi Spring Security — autentikasi & otorisasi.
 *
 * ✅ KETENTUAN — Security:
 *   - BCrypt untuk hashing password (tidak disimpan plain text)
 *   - Endpoint /api/** butuh autentikasi
 *   - Endpoint /h2-console, /auth/**, /api/public/** boleh diakses bebas
 *   - Default admin dibuat saat startup (CommandLineRunner)
 *
 * ✅ OOP PILAR — ABSTRACTION:
 *   @Configuration menyembunyikan kompleksitas setup security dari developer lain.
 *   Cukup satu class ini yang perlu diubah jika aturan security berubah.
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Detail implementasi BCrypt disembunyikan di balik PasswordEncoder interface.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * ✅ Security — Filter Chain: aturan akses setiap endpoint.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF untuk REST API (stateless)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**", "/auth/**"))

            // Izinkan H2 Console iframe
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))

            // Aturan otorisasi per endpoint
            .authorizeHttpRequests(auth -> auth
                // ✅ Public endpoints (tidak butuh login)
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // ✅ Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ✅ Semua /api/** butuh autentikasi
                .requestMatchers("/api/**").authenticated()

                // Lainnya boleh diakses
                .anyRequest().permitAll()
            )

            // HTTP Basic auth (untuk testing via Postman / JavaFX)
            .httpBasic(basic -> {})

            // Form login (opsional — untuk browser)
            .formLogin(form -> form
                .loginPage("/auth/login")
                .defaultSuccessUrl("/api/goals", true)
                .permitAll()
            )

            .logout(logout -> logout.logoutUrl("/auth/logout").permitAll());

        return http.build();
    }

    /**
     * ✅ Security — BCrypt Password Encoder.
     * Password tidak pernah disimpan dalam plain text.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuthenticationManager untuk keperluan login manual. */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ✅ Security — Seed data: buat akun default saat aplikasi pertama kali jalan.
     * username: admin | password: admin123
     * username: user  | password: user123
     */
    @Bean
    public CommandLineRunner seedDefaultUsers(AppUserRepository userRepository,
                                              PasswordEncoder encoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(new AppUser(
                    "admin",
                    encoder.encode("admin123"),
                    "ROLE_ADMIN"
                ));
                System.out.println("[Security] ✅ Default admin dibuat: admin / admin123");
            }
            if (!userRepository.existsByUsername("user")) {
                userRepository.save(new AppUser(
                    "user",
                    encoder.encode("user123"),
                    "ROLE_USER"
                ));
                System.out.println("[Security] ✅ Default user dibuat: user / user123");
            }
        };
    }
}
