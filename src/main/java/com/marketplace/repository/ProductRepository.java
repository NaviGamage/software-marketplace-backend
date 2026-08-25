package com.marketplace.repository;

import com.marketplace.entity.Product;
import com.marketplace.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"vendor", "category"})
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByFileSha256(String fileSha256);

    @EntityGraph(attributePaths = {"vendor", "category"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "category"})
    Page<Product> findByStatusAndCategoryId(ProductStatus status, Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "category"})
    Page<Product> findByVendorId(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "category"})
    Page<Product> findByStatusOrderByCreatedAtAsc(ProductStatus status, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}