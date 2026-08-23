package com.marketplace.repository;

import com.marketplace.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}