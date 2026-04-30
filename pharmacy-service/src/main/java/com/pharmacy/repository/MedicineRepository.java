package com.pharmacy.repository;

import com.pharmacy.entity.Medicine;
import com.pharmacy.enums.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByNameIgnoreCase(String name);

    List<Medicine> findByActiveTrue();

    List<Medicine> findByCategoryAndActiveTrue(MedicineCategory category);

    List<Medicine> findByRequiresPrescriptionAndActiveTrue(Boolean requiresPrescription);

    // Search by name (for autocomplete / prescription matching)
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND m.active = true")
    List<Medicine> searchByName(@Param("keyword") String keyword);

    // Find medicines that are low in stock (for admin dashboard)
    @Query("SELECT m FROM Medicine m WHERE m.stockQuantity <= :threshold AND m.active = true")
    List<Medicine> findLowStockMedicines(@Param("threshold") int threshold);

    // Match extracted medicine names (from OpenAI) to catalog
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) IN :names AND m.active = true")
    List<Medicine> findByNamesIn(@Param("names") List<String> names);
}
