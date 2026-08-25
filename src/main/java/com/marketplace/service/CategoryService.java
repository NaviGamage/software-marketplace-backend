package com.marketplace.service;

import com.marketplace.dto.request.CreateCategoryRequest;
import com.marketplace.dto.request.UpdateCategoryRequest;
import com.marketplace.dto.response.CategoryResponse;
import com.marketplace.entity.Category;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.DuplicateResourceException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("This slug is already in use");
        }

        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        if (!category.getSlug().equals(request.slug()) && categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("This slug is already in use");
        }

        category.setName(request.name());
        category.setSlug(request.slug());

        Category updated = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category because it contains active products");
        }

        categoryRepository.delete(category);
    }
}