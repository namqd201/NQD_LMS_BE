package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Product;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByCodeAndIsDeletedFalse(String code);

    Optional<Product> findByTargetEntityIdAndProductTypeAndIsDeletedFalse(UUID targetEntityId, ProductType productType);

    Page<Product> findByStatusAndIsDeletedFalse(ProductStatus status, Pageable pageable);

    Page<Product> findByProductTypeAndStatusAndIsDeletedFalse(ProductType productType, ProductStatus status, Pageable pageable);

    boolean existsByCode(String code);
}
