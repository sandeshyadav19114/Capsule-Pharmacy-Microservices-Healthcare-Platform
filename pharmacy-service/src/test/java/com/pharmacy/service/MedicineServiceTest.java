package com.pharmacy.service;

import com.pharmacy.entity.Medicine;
import com.pharmacy.enums.MedicineCategory;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MedicineService.
 * Coverage target: 80%+ (JaCoCo enforced via pom.xml).
 *
 * Pattern: Arrange → Act → Assert (AAA)
 * Mocking: Mockito — no Spring context loaded (fast tests)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicineService Unit Tests")
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine testMedicine;

    @BeforeEach
    void setUp() {
        testMedicine = Medicine.builder()
                .id(1L)
                .name("Amoxicillin 500mg")
                .manufacturer("Sun Pharma")
                .price(new BigDecimal("45.00"))
                .stockQuantity(100)
                .category(MedicineCategory.ANTIBIOTIC)
                .requiresPrescription(true)
                .active(true)
                .build();
    }

    // ── addMedicine ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should add medicine successfully when name is unique")
    void addMedicine_success() {
        when(medicineRepository.findByNameIgnoreCase("Amoxicillin 500mg"))
                .thenReturn(Optional.empty());
        when(medicineRepository.save(any(Medicine.class))).thenReturn(testMedicine);

        Medicine result = medicineService.addMedicine(testMedicine);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Amoxicillin 500mg");
        verify(medicineRepository).save(testMedicine);
    }

    @Test
    @DisplayName("Should throw exception when medicine name already exists")
    void addMedicine_duplicateName_throwsException() {
        when(medicineRepository.findByNameIgnoreCase("Amoxicillin 500mg"))
                .thenReturn(Optional.of(testMedicine));

        assertThatThrownBy(() -> medicineService.addMedicine(testMedicine))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(medicineRepository, never()).save(any());
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return medicine when ID exists")
    void getById_found() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));

        Medicine result = medicineService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Amoxicillin 500mg");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ID not found")
    void getById_notFound_throwsResourceNotFoundException() {
        when(medicineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicineService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── deductStock ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should deduct stock successfully when sufficient quantity available")
    void deductStock_success() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.save(any())).thenReturn(testMedicine);

        medicineService.deductStock(1L, 10);

        assertThat(testMedicine.getStockQuantity()).isEqualTo(90);
        verify(medicineRepository).save(testMedicine);
    }

    @Test
    @DisplayName("Should throw exception when stock is insufficient")
    void deductStock_insufficientStock_throwsException() {
        testMedicine.setStockQuantity(5);
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));

        assertThatThrownBy(() -> medicineService.deductStock(1L, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ── restoreStock ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should restore stock on order cancellation (Saga compensating tx)")
    void restoreStock_success() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.save(any())).thenReturn(testMedicine);

        medicineService.restoreStock(1L, 20);

        assertThat(testMedicine.getStockQuantity()).isEqualTo(120);
        verify(medicineRepository).save(testMedicine);
    }

    // ── deactivateMedicine ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should deactivate medicine — sets active=false")
    void deactivateMedicine_success() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(testMedicine));
        when(medicineRepository.save(any())).thenReturn(testMedicine);

        medicineService.deactivateMedicine(1L);

        assertThat(testMedicine.getActive()).isFalse();
        verify(medicineRepository).save(testMedicine);
    }

    // ── getAllActiveMedicines ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active medicines")
    void getAllActiveMedicines_returnsList() {
        when(medicineRepository.findByActiveTrue()).thenReturn(List.of(testMedicine));

        List<Medicine> result = medicineService.getAllActiveMedicines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // ── getLowStockMedicines ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should return medicines below stock threshold")
    void getLowStockMedicines_returnsList() {
        testMedicine.setStockQuantity(5);
        when(medicineRepository.findLowStockMedicines(10)).thenReturn(List.of(testMedicine));

        List<Medicine> result = medicineService.getLowStockMedicines(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockQuantity()).isLessThanOrEqualTo(10);
    }
}
