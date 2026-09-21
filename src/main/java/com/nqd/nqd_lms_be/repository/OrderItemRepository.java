package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdAndIsDeletedFalse(UUID orderId);
}
