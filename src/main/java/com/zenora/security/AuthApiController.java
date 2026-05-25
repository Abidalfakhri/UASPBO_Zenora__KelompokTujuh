package com.zenora.security;

import com.zenora.entity.AppUser;
import com.zenora.repository.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ✅ PR-4: REST Controller untuk autentikasi (register & login).
 *
 * ✅ KETENTUAN — Security:
 *   POST /auth/register — daftarkan user baru
 *   POST /auth/login    — login (verifikasi username/password)
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Logika encoding password disembunyikan oleh PasswordEncoder.
 *   Controller tidak tahu apakah pakai BCrypt atau algoritma lain.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthApiController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthApiController(AppUserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             AuthenticationManager authenticationManager) {
        this.userRepository        = userRepository;
        this.passwordEncoder       = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // ── POST /auth/register ────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Username '" + req.getUsername() + "' sudah dipakai"));
        }
        AppUser user = new AppUser(
            req.getUsername(),
            passwordEncoder.encode(req.getPassword()),
            "ROLE_USER"
        );
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "Registrasi berhasil", "username", req.getUsername()));
    }

    // ── POST /auth/login ───────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            return ResponseEntity.ok(Map.of(
                "message", "Login berhasil",
                "username", req.getUsername(),
                "info", "Gunakan Basic Auth di header untuk endpoint /api/**"
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Username atau password salah"));
        }
    }

    // ── Inner DTOs ─────────────────────────────────────────────────────────

    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 50) private String username;
        @NotBlank @Size(min = 6, max = 100) private String password;
        public String getUsername() { return username; }
        public void setUsername(String v) { this.username = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
    }

    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
        public String getUsername() { return username; }
        public void setUsername(String v) { this.username = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
    }
}
