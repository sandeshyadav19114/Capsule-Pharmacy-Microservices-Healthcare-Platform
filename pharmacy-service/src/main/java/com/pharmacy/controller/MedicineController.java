package com.pharmacy.controller;

import com.pharmacy.entity.Medicine;
import com.pharmacy.enums.MedicineCategory;
import com.pharmacy.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MedicineController — REST API for medicine catalog.
 *
 * Public:
 *   GET  /api/medicines              → list all active medicines
 *   GET  /api/medicines/{id}         → get medicine by ID
 *   GET  /api/medicines/search?q=    → search by name
 *   GET  /api/medicines/category/{c} → filter by category
 *
 * Admin only:
 *   POST   /api/medicines            → add medicine
 *   PUT    /api/medicines/{id}       → update medicine
 *   DELETE /api/medicines/{id}       → deactivate medicine
 *   GET    /api/medicines/low-stock  → medicines with stock ≤ threshold
 */
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
@Tag(name = "Medicine Catalog", description = "Browse and manage medicines")
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    @Operation(summary = "Get all active medicines")
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineService.getAllActiveMedicines());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get medicine by ID")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search medicines by name keyword")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String q) {
        return ResponseEntity.ok(medicineService.searchMedicines(q));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get medicines by category")
    public ResponseEntity<List<Medicine>> getByCategory(@PathVariable MedicineCategory category) {
        return ResponseEntity.ok(medicineService.getMedicinesByCategory(category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add new medicine (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Medicine> addMedicine(@Valid @RequestBody Medicine medicine) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicineService.addMedicine(medicine));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update medicine (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id,
                                                    @Valid @RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineService.updateMedicine(id, medicine));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate medicine (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deactivateMedicine(@PathVariable Long id) {
        medicineService.deactivateMedicine(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get low stock medicines (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Medicine>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(medicineService.getLowStockMedicines(threshold));
    }
}
