package com.zenora.api;

import com.zenora.dto.DebtRequestDto;
import com.zenora.entity.DebtEntity;
import com.zenora.repository.DebtRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
@CrossOrigin(origins = "*")
public class DebtApiController {

    private final DebtRepository debtRepository;

    public DebtApiController(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    // ── GET /api/debts ─────────────────────────────────────────────────────
    /** Ambil semua hutang milik user yang sedang login. */
    @GetMapping
    public ResponseEntity<List<DebtEntity>> getAllDebts() {
        List<DebtEntity> debts = debtRepository.findByOwnerUsernameOrderByCreatedAtAsc(currentUser());
        return ResponseEntity.ok(debts);
    }

    // ── POST /api/debts ────────────────────────────────────────────────────
    /** Tambah hutang baru. */
    @PostMapping
    public ResponseEntity<DebtEntity> createDebt(@Valid @RequestBody DebtRequestDto req) {
        DebtEntity entity = new DebtEntity();
        entity.setOwnerUsername(currentUser());
        applyRequest(entity, req);
        DebtEntity saved = debtRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── PUT /api/debts/{id} ────────────────────────────────────────────────
    /** Update data hutang (nama, APR, minimum payment) atau catat setoran (balance, totalPaid). */
    @PutMapping("/{id}")
    public ResponseEntity<DebtEntity> updateDebt(
            @PathVariable String id,
            @Valid @RequestBody DebtRequestDto req) {

        String me = currentUser();
        return debtRepository.findById(id).map(entity -> {
            if (!entity.getOwnerUsername().equals(me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<DebtEntity>build();
            }
            applyRequest(entity, req);
            return ResponseEntity.ok(debtRepository.save(entity));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /api/debts/{id} ─────────────────────────────────────────────
    /** Hapus satu hutang. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDebt(@PathVariable String id) {
        String me = currentUser();
        DebtEntity entity = debtRepository.findById(id).orElse(null);
        if (entity == null) return ResponseEntity.notFound().build();
        if (!entity.getOwnerUsername().equals(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        debtRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── DELETE /api/debts ──────────────────────────────────────────────────
    /** Hapus semua hutang milik user yang sedang login. */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDebts() {
        debtRepository.deleteByOwnerUsername(currentUser());
        return ResponseEntity.noContent().build();
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private void applyRequest(DebtEntity entity, DebtRequestDto req) {
        entity.setName(req.getName());
        entity.setBalance(req.getBalance());
        entity.setOriginalBalance(req.getOriginalBalance() != null
                ? req.getOriginalBalance() : req.getBalance());
        entity.setAprPercent(req.getAprPercent() != null ? req.getAprPercent() : 0.0);
        entity.setMinimumPayment(req.getMinimumPayment() != null ? req.getMinimumPayment() : 0.0);
        entity.setTotalPaid(req.getTotalPaid() != null ? req.getTotalPaid() : 0.0);
    }

    private static String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null || a.getName() == null) ? "anonymous" : a.getName();
    }
}
