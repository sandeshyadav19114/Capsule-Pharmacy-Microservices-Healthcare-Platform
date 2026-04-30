package com.pharmacy.service;

import com.pharmacy.entity.Medicine;
import com.pharmacy.enums.MedicineCategory;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MedicineService — manages the medicine catalog.
 *
 * Admin operations: addMedicine, updateMedicine, deactivateMedicine, updateStock
 * Public operations: getAllMedicines, searchMedicines, getMedicinesByCategory
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public Medicine addMedicine(Medicine medicine) {
        log.info("Adding new medicine: {}", medicine.getName());
        if (medicineRepository.findByNameIgnoreCase(medicine.getName()).isPresent()) {
            throw new IllegalArgumentException("Medicine already exists: " + medicine.getName());
        }
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Long id, Medicine updated) {
        Medicine existing = getById(id);
        existing.setPrice(updated.getPrice());
        existing.setStockQuantity(updated.getStockQuantity());
        existing.setDescription(updated.getDescription());
        existing.setActive(updated.getActive());
        log.info("Updated medicine: {}", id);
        return medicineRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Medicine> getAllActiveMedicines() {
        return medicineRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String keyword) {
        return medicineRepository.searchByName(keyword);
    }

    @Transactional(readOnly = true)
    public List<Medicine> getMedicinesByCategory(MedicineCategory category) {
        return medicineRepository.findByCategoryAndActiveTrue(category);
    }

    @Transactional(readOnly = true)
    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
    }

    /**
     * Deducts stock after order is confirmed.
     * Called after Kafka PaymentCompletedEvent (SUCCESS).
     */
    public void deductStock(Long medicineId, int quantity) {
        Medicine medicine = getById(medicineId);
        if (medicine.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for: " + medicine.getName());
        }
        medicine.setStockQuantity(medicine.getStockQuantity() - quantity);
        medicineRepository.save(medicine);
        log.info("Deducted {} units from medicine: {}", quantity, medicine.getName());
    }

    /**
     * Restores stock on order cancellation (Saga compensating transaction).
     */
    public void restoreStock(Long medicineId, int quantity) {
        Medicine medicine = getById(medicineId);
        medicine.setStockQuantity(medicine.getStockQuantity() + quantity);
        medicineRepository.save(medicine);
        log.info("Restored {} units to medicine: {}", quantity, medicine.getName());
    }

    public void deactivateMedicine(Long id) {
        Medicine medicine = getById(id);
        medicine.setActive(false);
        medicineRepository.save(medicine);
        log.info("Deactivated medicine: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Medicine> getLowStockMedicines(int threshold) {
        return medicineRepository.findLowStockMedicines(threshold);
    }
}
