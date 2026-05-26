package com.zenora.security;

import com.zenora.entity.AppUser;
import com.zenora.repository.AppUserRepository;
import com.zenora.repository.ContributionRepository;
import com.zenora.repository.DebtRepository;
import com.zenora.repository.GoalRepository;
import com.zenora.repository.UserProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthApiController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserProfileRepository userProfileRepository;
    private final GoalRepository goalRepository;
    private final DebtRepository debtRepository;
    private final ContributionRepository contributionRepository;

    public AuthApiController(AppUserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             AuthenticationManager authenticationManager,
                             UserProfileRepository userProfileRepository,
                             GoalRepository goalRepository,
                             DebtRepository debtRepository,
                             ContributionRepository contributionRepository) {
        this.userRepository         = userRepository;
        this.passwordEncoder        = passwordEncoder;
        this.authenticationManager  = authenticationManager;
        this.userProfileRepository  = userProfileRepository;
        this.goalRepository         = goalRepository;
        this.debtRepository         = debtRepository;
        this.contributionRepository = contributionRepository;
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

    // ── DELETE /auth/account ───────────────────────────────────────────────
    /** Hapus akun user yang sedang login + semua datanya (profil, goal, debt, contribution). */
    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> deleteAccount(@RequestBody(required = false) DeleteAccountRequest req) {
        String me = currentUsername();
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Tidak ada sesi aktif"));
        }

        AppUser user = userRepository.findByUsername(me).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User tidak ditemukan"));
        }

        // Konfirmasi password (kalau dikirim) — direkomendasikan untuk keamanan.
        if (req != null && req.getPassword() != null && !req.getPassword().isBlank()) {
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Password salah"));
            }
        }

        // Hapus data terkait dulu (FK-safe order).
        contributionRepository.deleteByOwnerUsername(me);
        debtRepository.deleteByOwnerUsername(me);
        goalRepository.deleteByOwnerUsername(me);
        userProfileRepository.deleteByOwnerUsername(me);
        userRepository.delete(user);

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Akun berhasil dihapus"));
    }

    private static String currentUsername() {
        org.springframework.security.core.Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName())) return null;
        return a.getName();
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

    public static class DeleteAccountRequest {
        private String password;
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
    }
}
