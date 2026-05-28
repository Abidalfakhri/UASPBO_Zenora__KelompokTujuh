package com.zenora.security;

import com.zenora.entity.AppUser;
import com.zenora.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http

            // Disable CSRF untuk REST API + H2 Console
            .csrf(csrf -> csrf.disable())

            // H2 Console iframe support
            .headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin())
            )

            // Stateless API — Basic Auth, jangan buat HTTP session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization Rules
            .authorizeHttpRequests(auth -> auth

                // Endpoint publik (register & login saja)
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/h2-console/**",
                    "/api/public/**"
                ).permitAll()

                // Endpoint /auth/account (hapus akun, dll) wajib login
                .requestMatchers("/auth/account").authenticated()

                // Admin only
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")

                // Semua API lain harus login
                .requestMatchers("/api/**")
                .authenticated()

                // Sisanya bebas
                .anyRequest()
                .permitAll()
            )

            // BASIC AUTH
            .httpBasic(Customizer.withDefaults());

        // HAPUS formLogin()
        // karena bentrok dengan REST API /auth/login

        return http.build();
    }

    /**
     * BCrypt Password Encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    /**
     * Seed default users
     */
    @Bean
    @org.springframework.context.annotation.Profile("!prod")
    public CommandLineRunner seedDefaultUsers(
            AppUserRepository userRepository,
            PasswordEncoder encoder) {

        return args -> {

            if (!userRepository.existsByUsername("admin")) {

                userRepository.save(
                    new AppUser(
                        "admin",
                        encoder.encode("admin123"),
                        "ROLE_ADMIN"
                    )
                );

                System.out.println(
                    "[Security]  Default admin dibuat: admin / admin123"
                );
            }

            if (!userRepository.existsByUsername("user")) {

                userRepository.save(
                    new AppUser(
                        "user",
                        encoder.encode("user123"),
                        "ROLE_USER"
                    )
                );

                System.out.println(
                    "[Security]  Default user dibuat: user / user123"
                );
            }
        };
    }
}
