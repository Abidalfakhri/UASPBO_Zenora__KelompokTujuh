package com.zenora.api;

import com.zenora.dto.UserProfileRequestDto;
import com.zenora.entity.UserProfileEntity;
import com.zenora.repository.UserProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class UserProfileApiController {

    private final UserProfileRepository userProfileRepository;

    public UserProfileApiController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    // ── GET /api/profile ───────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<UserProfileEntity> getProfile() {
        return userProfileRepository.findFirstByOwnerUsernameOrderByCreatedAtAsc(currentUser())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── POST /api/profile ──────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<UserProfileEntity> createProfile(
            @Valid @RequestBody UserProfileRequestDto req) {

        String me = currentUser();
        // Cegah duplikasi: kalau sudah ada profil milik user ini, update saja.
        UserProfileEntity entity = userProfileRepository
                .findFirstByOwnerUsernameOrderByCreatedAtAsc(me)
                .orElseGet(UserProfileEntity::new);
        entity.setOwnerUsername(me);
        applyRequest(entity, req);
        UserProfileEntity saved = userProfileRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── PUT /api/profile/{id} ──────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileEntity> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody UserProfileRequestDto req) {

        String me = currentUser();
        return userProfileRepository.findById(id).map(entity -> {
            if (entity.getOwnerUsername() != null && !entity.getOwnerUsername().equals(me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<UserProfileEntity>build();
            }
            entity.setOwnerUsername(me);
            applyRequest(entity, req);
            return ResponseEntity.ok(userProfileRepository.save(entity));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /api/profile/{id} ───────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        String me = currentUser();
        return userProfileRepository.findById(id).map(entity -> {
            if (entity.getOwnerUsername() != null && !entity.getOwnerUsername().equals(me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }
            userProfileRepository.delete(entity);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private void applyRequest(UserProfileEntity entity, UserProfileRequestDto req) {
        entity.setName(req.getName());
        entity.setAge(req.getAge());
        entity.setMonthlyIncome(req.getMonthlyIncome());
        entity.setMonthlyExpense(req.getMonthlyExpense());
        entity.setMonthlyCapacityOverride(
                req.getMonthlyCapacityOverride() == null ? 0.0 : req.getMonthlyCapacityOverride());
        entity.setEmergencyMonths(req.getEmergencyMonths());
        entity.setHouseholdStatus(req.getHouseholdStatus());
        entity.setInflationPct(req.getInflationPct());
    }

    private static String currentUser() {
        org.springframework.security.core.Authentication a =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (a == null || a.getName() == null) ? "anonymous" : a.getName();
    }
}
