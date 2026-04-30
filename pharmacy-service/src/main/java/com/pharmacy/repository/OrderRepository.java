package com.pharmacy.repository;

import com.pharmacy.entity.Order;
import com.pharmacy.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndPatientId(Long id, Long patientId);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Long countOrdersBetween(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.patientId = :patientId AND o.status = :status")
    List<Order> findByPatientIdAndStatus(@Param("patientId") Long patientId,
                                         @Param("status") OrderStatus status);
}